package com.britam.insureclaim.kyc

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface KycVerificationRepository : JpaRepository<KycVerification, Long> {

	fun findByUserIdAndDocumentType(userId: Long, documentType: KycDocumentType): Optional<KycVerification>

	fun findFirstByUserIdOrderBySubmittedAtDesc(userId: Long): Optional<KycVerification>

	/** Full submission trail for the customer, newest first. */
	fun findByUserIdOrderBySubmittedAtDesc(userId: Long): List<KycVerification>

	fun existsByUserIdAndStatus(userId: Long, status: KycStatus): Boolean

	fun findByStatusOrderBySubmittedAtAsc(status: KycStatus, pageable: Pageable): Page<KycVerification>

	fun countByStatusOrderBySubmittedAtAsc(status: KycStatus): Long

	@Query(
		"""
		SELECT k FROM KycVerification k
		WHERE k.user.id = :userId AND k.status = :status
		ORDER BY k.submittedAt DESC
		""",
	)
	fun findByUserAndStatus(
		@Param("userId") userId: Long,
		@Param("status") status: KycStatus,
	): List<KycVerification>

	@Query(
		"""
		SELECT COUNT(k) FROM KycVerification k
		WHERE k.status = :status
		""",
	)
	fun countByStatus(@Param("status") status: KycStatus): Long

	@Query(
		"""
		SELECT AVG((EXTRACT(EPOCH FROM k.verifiedAt) - EXTRACT(EPOCH FROM k.submittedAt)) / 3600)
		FROM KycVerification k
		WHERE k.verifiedAt IS NOT NULL AND k.submittedAt >= :since
		""",
	)
	fun averageVerificationHoursSince(@Param("since") since: java.time.Instant): Double?
}
