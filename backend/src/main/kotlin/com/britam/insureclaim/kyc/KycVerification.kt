package com.britam.insureclaim.kyc

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.common.BaseEntity
import com.britam.insureclaim.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

enum class KycDocumentType {
	NIC,
	PASSPORT,
	DRIVING_LICENSE;

	val displayLabel: String
		get() = when (this) {
			NIC -> "National Identity Card"
			PASSPORT -> "Passport"
			DRIVING_LICENSE -> "Driving Licence"
		}

	val validityYears: Int
		get() = when (this) {
			NIC, DRIVING_LICENSE -> 10
			PASSPORT -> 10
		}
}

enum class KycStatus {
	PENDING,
	IN_REVIEW,
	VERIFIED,
	REJECTED,
	EXPIRED;

	val isSettled: Boolean
		get() = this == VERIFIED || this == REJECTED

	val isBlocking: Boolean
		get() = this == PENDING || this == IN_REVIEW

	val label: String
		get() = when (this) {
			PENDING -> "Awaiting verification"
			IN_REVIEW -> "Under review"
			VERIFIED -> "Verified"
			REJECTED -> "Rejected"
			EXPIRED -> "Expired"
		}
}

@Entity
@Table(name = "kyc_verifications")
class KycVerification(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	var user: User = User().apply { id = 0L },

	/** Optional link to the claim that triggered this KYC request. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "claim_id")
	var claim: Claim? = null,

	init {
		if (claim != null) claim.id = claim.id ?: 0L
	}


	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 32)
	var documentType: KycDocumentType = KycDocumentType.NIC,

	@Column(name = "document_number", nullable = false, length = 64)
	var documentNumber: String = "",

	@Column(name = "full_name_on_doc", nullable = false, length = 160)
	var fullNameOnDoc: String = "",

	@Column(name = "date_of_birth")
	var dateOfBirth: LocalDate? = null,

	@Column(name = "front_image_path", length = 512)
	var frontImagePath: String? = null,

	@Column(name = "back_image_path", length = 512)
	var backImagePath: String? = null,

	@Column(name = "selfie_path", length = 512)
	var selfiePath: String? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	var status: KycStatus = KycStatus.PENDING,

	@Column(name = "failure_reason", length = 255)
	var failureReason: String? = null,

	/** 0-1 confidence returned by the document verification provider. */
	@Column(name = "confidence_score", precision = 5, scale = 4)
	var confidenceScore: BigDecimal? = null,

	@Column(name = "verified_by")
	var verifiedBy: Long? = null,

	@Column(name = "verified_at")
	var verifiedAt: Instant? = null,

	@Column(name = "expires_at")
	var expiresAt: LocalDate? = null,

	/** Set when staff rejected the document but let the customer try again. */
	@Column(name = "resubmission_allowed", nullable = false)
	var resubmissionAllowed: Boolean = false,

	@CreationTimestamp
	@Column(name = "submitted_at", nullable = false, updatable = false)
	var submittedAt: Instant = Instant.now(),

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.now(),
)
{

	/**
	 * Offline name check used when no verification provider is configured. The
	 * provider path replaces this with a document-to-selfie comparison.
	 */
	fun nameMatchesAccount(accountName: String): Boolean =
		normalise(fullNameOnDoc) == normalise(accountName)

	fun structurallyValidNumber(): Boolean = when (documentType) {
		KycDocumentType.NIC -> documentNumber.matches(Regex("^(\\d{9}[VXvx]|\\d{12})$"))
		KycDocumentType.PASSPORT -> documentNumber.matches(Regex("^[A-Z]{1,2}\\d{6,9}$"))
		KycDocumentType.DRIVING_LICENSE -> documentNumber.matches(Regex("^[A-Z]{1,2}\\d{6,12}$"))
	}

	/** A verified KYC record stops counting once the document expires. */
	fun isValidOn(today: LocalDate = LocalDate.now()): Boolean =
		status == KycStatus.VERIFIED && (expiresAt == null || !expiresAt!!.isBefore(today))

	/**
	 * How long a fresh verification stays valid. Measured from the verification
	 * date rather than the date of birth, otherwise a document would be born
	 * expired for any adult holder.
	 */
	fun effectiveExpiry(from: LocalDate = LocalDate.now()): LocalDate =
		expiresAt ?: from.plusYears(documentType.validityYears.toLong())

	/**
	 * Whether a new submission may replace this record. A parked submission is
	 * waiting on staff, and a closed rejection is only reopenable when the
	 * reviewer allowed a correction.
	 */
	fun reopenable(): Boolean = when (status) {
		KycStatus.PENDING -> false
		KycStatus.REJECTED -> resubmissionAllowed
		else -> true
	}

	private fun normalise(value: String): String =
		value.trim().lowercase().replace(Regex("\\s+"), " ")
}
