package com.britam.insureclaim.fraud

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface FraudAlertRepository : JpaRepository<FraudAlert, Long> {

	fun findByClaimIdOrderByCreatedAtDesc(claimId: Long): List<FraudAlert>

	fun findByClaimIdAndRuleCode(claimId: Long, ruleCode: String): Optional<FraudAlert>

	fun existsByClaimIdAndRuleCode(claimId: Long, ruleCode: String): Boolean

	fun findByStatusOrderByCreatedAtDesc(status: FraudAlertStatus, pageable: Pageable): Page<FraudAlert>

	fun findBySeverityOrderByCreatedAtDesc(severity: FraudSeverity, pageable: Pageable): Page<FraudAlert>

	fun countByStatus(status: FraudAlertStatus): Long

	fun countBySeverityIn(severities: Collection<FraudSeverity>): Long

	@Query("SELECT a.status, COUNT(a) FROM FraudAlert a GROUP BY a.status")
	fun countGroupedByStatus(): List<Array<Any>>

	@Query("SELECT a.severity, COUNT(a) FROM FraudAlert a GROUP BY a.severity")
	fun countGroupedBySeverity(): List<Array<Any>>

	@Query(
		"""
		SELECT a.category, COUNT(a) FROM FraudAlert a
		WHERE a.createdAt >= :since GROUP BY a.category
		""",
	)
	fun countGroupedByCategorySince(@Param("since") since: java.time.Instant): List<Array<Any>>
}
