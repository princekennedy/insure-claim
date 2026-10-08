package com.britam.insureclaim.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Append-only record of one @Auditable endpoint invocation. Plain columns
 * (no BaseEntity) because the row carries its own actor and timestamp and
 * must never be updated after insert.
 */
@Entity
@Table(name = "audit_logs")
class AuditLog(
	@Column(name = "actor_id")
	var actorId: Long? = null,

	@Column(name = "actor_email", length = 255)
	var actorEmail: String? = null,

	@Column(name = "actor_role", length = 64)
	var actorRole: String? = null,

	@Column(name = "action", nullable = false, length = 64)
	var action: String = "",

	@Column(name = "description", nullable = false, length = 255)
	var description: String = "",

	@Column(name = "entity_type", length = 64)
	var entityType: String? = null,

	@Column(name = "entity_id", length = 64)
	var entityId: String? = null,

	@Column(name = "request_method", nullable = false, length = 8)
	var requestMethod: String = "GET",

	@Column(name = "request_path", nullable = false, length = 255)
	var requestPath: String = "",

	@Column(name = "status", nullable = false)
	var status: Int = 200,

	@Column(name = "success", nullable = false)
	var success: Boolean = true,

	@Column(name = "detail", length = 512)
	var detail: String? = null,

	@Column(name = "ip_address", length = 64)
	var ipAddress: String? = null,
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null

	@Column(name = "created_at", nullable = false, updatable = false)
	var createdAt: Instant = Instant.now()
}
