package com.britam.insureclaim.claim

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

interface ClaimRepository :
	JpaRepository<Claim, Long>,
	JpaSpecificationExecutor<Claim> {

	fun findByClaimNumber(claimNumber: String): Optional<Claim>

	@Query(
		"""
		SELECT c FROM Claim c
		JOIN FETCH c.policy p
		JOIN FETCH c.vehicle
		JOIN FETCH c.customer
		WHERE c.id = :claimId
		""",
	)
	fun findByIdWithDetails(@Param("claimId") claimId: Long): Optional<Claim>

	fun findByCustomerIdOrderBySubmittedAtDesc(customerId: Long, pageable: Pageable): Page<Claim>

	fun findByCustomerIdAndStatusInOrderBySubmittedAtDesc(
		customerId: Long,
		statuses: Collection<ClaimStatus>,
		pageable: Pageable,
	): Page<Claim>

	fun countByCustomerId(customerId: Long): Long

	fun countByCustomerIdAndStatusIn(customerId: Long, statuses: Collection<ClaimStatus>): Long

	fun existsByClaimNumber(claimNumber: String): Boolean

	/** Used by the fraud ruleset to spot claims filed unusually close together. */
	@Query(
		"""
		SELECT COUNT(c) FROM Claim c
		WHERE c.customer.id = :customerId AND c.submittedAt >= :since
		""",
	)
	fun countByCustomerSince(
		@Param("customerId") customerId: Long,
		@Param("since") since: Instant,
	): Long

	@Query(
		"""
		SELECT COUNT(c) FROM Claim c
		WHERE c.customer.id = :customerId
			AND c.status = :rejectedStatus
			AND c.submittedAt >= :since
		""",
	)
	fun countRejectedByCustomerSince(
		@Param("customerId") customerId: Long,
		@Param("rejectedStatus") rejectedStatus: ClaimStatus,
		@Param("since") since: Instant,
	): Long

	@Query("SELECT COALESCE(SUM(c.approvedAmount), 0) FROM Claim c WHERE c.customer.id = :customerId AND c.status = :settledStatus")
	fun sumSettledByCustomer(
		@Param("customerId") customerId: Long,
		@Param("settledStatus") settledStatus: ClaimStatus,
	): BigDecimal?

	@Query("SELECT COALESCE(SUM(c.estimatedAmount), 0) FROM Claim c WHERE c.customer.id = :customerId AND c.estimatedAmount IS NOT NULL")
	fun sumEstimatedByCustomer(@Param("customerId") customerId: Long): BigDecimal?

	// ---------------------------------------------------------- analytics ----

	@Query("SELECT c.status, COUNT(c) FROM Claim c GROUP BY c.status")
	fun countGroupedByStatus(): List<Array<Any>>

	@Query(
		"""
		SELECT COUNT(c) FROM Claim c
		WHERE c.submittedAt >= :since
		""",
	)
	fun countSubmittedSince(@Param("since") since: Instant): Long

	@Query(
		"""
		SELECT COALESCE(SUM(c.approvedAmount), 0) FROM Claim c
		WHERE c.settledAt BETWEEN :from AND :to
		""",
	)
	fun sumSettledBetween(
		@Param("from") from: Instant,
		@Param("to") to: Instant,
	): BigDecimal?

	@Query(
		"""
		SELECT COALESCE(SUM(c.excessPaid), 0) FROM Claim c
		WHERE c.settledAt BETWEEN :from AND :to
		""",
	)
	fun sumExcessBetween(
		@Param("from") from: Instant,
		@Param("to") to: Instant,
	): BigDecimal?

	@Query(
		"""
		SELECT c FROM Claim c
		WHERE c.submittedAt >= :since AND c.settledAt IS NOT NULL
		""",
	)
	fun findSettledSince(@Param("since") since: Instant): List<Claim>

	@Query(
		"""
		SELECT AVG((EXTRACT(EPOCH FROM c.settledAt) - EXTRACT(EPOCH FROM c.submittedAt)) / 3600)
		FROM Claim c
		WHERE c.settledAt IS NOT NULL AND c.submittedAt >= :since
		""",
	)
	fun averageSettlementHoursSince(@Param("since") since: Instant): Double?

	@Query(
		"""
		SELECT c.incidentType, COUNT(c) FROM Claim c
		WHERE c.submittedAt >= :since GROUP BY c.incidentType
		""",
	)
	fun countGroupedByIncidentTypeSince(
		@Param("since") since: Instant,
	): List<Array<Any>>

	@Query(
		"""
		SELECT FUNCTION('date_trunc', 'day', c.submittedAt), COUNT(c)
		FROM Claim c
		WHERE c.submittedAt >= :since
		GROUP BY FUNCTION('date_trunc', 'day', c.submittedAt)
		ORDER BY FUNCTION('date_trunc', 'day', c.submittedAt)
		""",
	)
	fun countPerDaySince(@Param("since") since: Instant): List<Array<Any>>

	@Query(
		"""
		SELECT COUNT(c) FROM Claim c
		WHERE c.status NOT IN :excludedStatuses
		""",
	)
	fun countExcluding(
		@Param("excludedStatuses") excludedStatuses: Collection<ClaimStatus>,
	): Long

	/** Claims whose incident date precedes today, i.e. notification windows. */
	@Query(
		"""
		SELECT COUNT(c) FROM Claim c
		WHERE c.status = :status
			AND c.incidentDate < :today
		""",
	)
	fun countByStatusWithIncidentBefore(
		@Param("status") status: ClaimStatus,
		@Param("today") today: Instant,
	): Long

	@Query(
		"""
		SELECT COUNT(c) FROM Claim c
		WHERE c.isFraudFlagged = true AND c.status NOT IN :terminalStatuses
		""",
	)
	fun countFraudFlaggedExcluding(
		@Param("terminalStatuses") terminalStatuses: Collection<ClaimStatus>,
	): Long
}

interface ClaimDocumentRepository : JpaRepository<ClaimDocument, Long> {
	fun findByClaimIdOrderByUploadedAtDesc(claimId: Long): List<ClaimDocument>
	fun deleteByClaimId(claimId: Long)
	fun existsByClaimIdAndDocumentType(claimId: Long, documentType: ClaimDocumentType): Boolean
}

interface ClaimStatusEventRepository : JpaRepository<ClaimStatusEvent, Long> {
	fun findByClaimIdOrderByOccurredAtAsc(claimId: Long): List<ClaimStatusEvent>
	fun findByClaimIdAndVisibleToCustomerTrueOrderByOccurredAtAsc(claimId: Long): List<ClaimStatusEvent>
}

interface ClaimPublicTokenRepository : JpaRepository<ClaimPublicToken, Long> {
	fun findByToken(token: String): Optional<ClaimPublicToken>
	fun findByClaimId(claimId: Long): Optional<ClaimPublicToken>
}
