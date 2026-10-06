package com.britam.insureclaim.claim

import com.britam.insureclaim.common.AuditContext
import com.britam.insureclaim.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant

/**
 * Immutable audit row for every claim status change. Customers see only the
 * subset flagged [visibleToCustomer] so internal notes stay internal.
 */
@Entity
@Table(name = "claim_status_events")
class ClaimStatusEvent(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "claim_id", nullable = false)
	var claim: Claim = Claim(),

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 32)
	var fromStatus: ClaimStatus? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false, length = 32)
	var toStatus: ClaimStatus = ClaimStatus.SUBMITTED,

	@Column(name = "note", length = 500)
	var note: String? = null,

	@Column(name = "actor_id")
	var actorId: Long? = null,

	@Column(name = "actor_label", nullable = false, length = 160)
	var actorLabel: String = "System",

	@Column(name = "occurred_at", nullable = false)
	var occurredAt: Instant = Instant.now(),

	@Column(name = "visible_to_customer", nullable = false)
	var visibleToCustomer: Boolean = true,
) : BaseEntity()

@Entity
@Table(name = "claim_documents")
class ClaimDocument(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "claim_id", nullable = false)
	var claim: Claim = Claim(),

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 40)
	var documentType: ClaimDocumentType = ClaimDocumentType.DAMAGE_PHOTO,

	@Column(name = "file_name", nullable = false, length = 255)
	var fileName: String = "",

	@Column(name = "content_type", nullable = false, length = 120)
	var contentType: String = "application/octet-stream",

	@Column(name = "size_bytes", nullable = false)
	var sizeBytes: Long = 0,

	@Column(name = "storage_path", nullable = false, length = 512)
	var storagePath: String = "",

	/** SHA-256 of the stored bytes, used to spot duplicate submissions. */
	@Column(name = "checksum", length = 64)
	var checksum: String? = null,

	@Column(name = "uploaded_at", nullable = false)
	var uploadedAt: Instant = Instant.now(),
) : BaseEntity() {

	val sizeLabel: String
		get() = when {
			sizeBytes >= 1_048_576 -> String.format("%.1f MB", sizeBytes / 1_048_576.0)
			sizeBytes >= 1024 -> String.format("%.0f KB", sizeBytes / 1024.0)
			else -> "$sizeBytes B"
		}
}

/**
 * Short-lived opaque token that lets a customer track a claim without an
 * account. Keyed on the claim id rather than a surrogate key, and deleted
 * along with the claim, so a token never outlives it.
 */
@Entity
@Table(name = "claim_public_tokens")
class ClaimPublicToken(
	@Id
	@Column(name = "claim_id")
	var claimId: Long = 0L,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "claim_id", insertable = false, updatable = false)
	var claim: Claim = Claim(),

	@Column(name = "token", nullable = false, unique = true, length = 64)
	var token: String = "",

	@Column(name = "expires_at", nullable = false)
	var expiresAt: Instant = Instant.now(),
) {

	@Column(name = "created_by")
	var createdBy: Long? = null

	@Column(name = "updated_by")
	var updatedBy: Long? = null

	@PrePersist
	protected fun stampCreatedBy() {
		val actor = AuditContext.actorId()
		if (createdBy == null) createdBy = actor
		if (updatedBy == null) updatedBy = actor ?: createdBy
	}

	@PreUpdate
	protected fun stampUpdatedBy() {
		AuditContext.actorId()?.let { updatedBy = it }
	}

	fun isUsable(now: Instant = Instant.now()): Boolean = expiresAt.isAfter(now)
}
