package com.britam.insureclaim.audit

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(name = "AuditLogResponse", description = "One recorded activity")
data class AuditLogResponse(
	val id: Long,
	val actorId: Long?,
	val actorEmail: String?,
	val actorRole: String?,
	val action: String,
	val description: String,
	val entityType: String?,
	val entityId: String?,
	val requestMethod: String,
	val requestPath: String,
	val status: Int,
	val success: Boolean,
	val detail: String?,
	val ipAddress: String?,
	val createdAt: Instant,
) {
	companion object {
		fun from(entry: AuditLog): AuditLogResponse = AuditLogResponse(
			id = entry.id ?: 0L,
			actorId = entry.actorId,
			actorEmail = entry.actorEmail,
			actorRole = entry.actorRole,
			action = entry.action,
			description = entry.description,
			entityType = entry.entityType,
			entityId = entry.entityId,
			requestMethod = entry.requestMethod,
			requestPath = entry.requestPath,
			status = entry.status,
			success = entry.success,
			detail = entry.detail,
			ipAddress = entry.ipAddress,
			createdAt = entry.createdAt,
		)
	}
}
