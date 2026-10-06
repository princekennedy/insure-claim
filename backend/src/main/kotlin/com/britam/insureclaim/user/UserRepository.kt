package com.britam.insureclaim.user

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {

	fun findByEmailIgnoreCase(email: String): Optional<User>

	fun existsByEmailIgnoreCase(email: String): Boolean

	@Query(
		"""
		SELECT u FROM User u
		WHERE (
			LOWER(u.fullName) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%'))
			OR LOWER(u.email) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%'))
			OR LOWER(COALESCE(u.nic, '')) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%'))
		)
		AND u.role = COALESCE(:role, u.role)
		""",
	)
	fun search(
		@Param("query") query: String?,
		@Param("role") role: Role?,
		pageable: Pageable,
	): Page<User>

	@Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.enabled = true")
	fun countActiveByRole(@Param("role") role: Role): Long
}

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {

	fun findByTokenHash(tokenHash: String): Optional<RefreshToken>

	@Query("SELECT t FROM RefreshToken t WHERE t.user.id = :userId AND t.revokedAt IS NULL")
	fun findActiveByUser(@Param("userId") userId: Long): List<RefreshToken>

	fun deleteByExpiresAtBefore(cutoff: Instant): Long
}
