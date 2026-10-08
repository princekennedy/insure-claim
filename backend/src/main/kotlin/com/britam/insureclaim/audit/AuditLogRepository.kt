package com.britam.insureclaim.audit

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AuditLogRepository : JpaRepository<AuditLog, Long> {

	@Query(
		"""
		SELECT a FROM AuditLog a
		WHERE LOWER(CONCAT(a.action, ' ', a.description, ' ', COALESCE(a.actorEmail, ''), ' ',
			COALESCE(a.entityId, ''), ' ', a.requestPath))
			LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%'))
		AND a.action = COALESCE(:action, a.action)
		AND (:success IS NULL OR a.success = :success)
		AND (:from IS NULL OR a.createdAt >= :from)
		AND (:to IS NULL OR a.createdAt < :to)
		ORDER BY a.createdAt DESC
		""",
	)
	fun search(
		@Param("query") query: String?,
		@Param("action") action: String?,
		@Param("success") success: Boolean?,
		@Param("from") from: Instant?,
		@Param("to") to: Instant?,
		pageable: Pageable,
	): Page<AuditLog>
}
