package com.britam.insureclaim.feedback

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface GarageFeedbackRepository : JpaRepository<GarageFeedback, Long> {

	fun findByClaimId(claimId: Long): Optional<GarageFeedback>

	fun existsByClaimId(claimId: Long): Boolean

	fun findByCustomerIdOrderByCreatedAtDesc(customerId: Long, pageable: Pageable): Page<GarageFeedback>

	fun findByGarageIdOrderByCreatedAtDesc(garageId: Long, pageable: Pageable): Page<GarageFeedback>

	@Query("SELECT f FROM GarageFeedback f JOIN FETCH f.garage WHERE f.customer.id = :customerId")
	fun findByCustomerWithGarage(
		@Param("customerId") customerId: Long,
		pageable: Pageable,
	): Page<GarageFeedback>

	@Query(
		"""
		SELECT COALESCE(SUM(f.overallRating), 0), COUNT(f) FROM GarageFeedback f
		WHERE f.garage.id = :garageId
		""",
	)
	fun ratingSummaryForGarage(@Param("garageId") garageId: Long): List<Any>

	@Query(
		"""
		SELECT COUNT(f) FROM GarageFeedback f
		WHERE f.garage.id = :garageId
			AND (f.overallRating <= :lowRating OR f.recommendAgain = FALSE)
		""",
	)
	fun countComplaintsForGarage(
		@Param("garageId") garageId: Long,
		@Param("lowRating") lowRating: java.math.BigDecimal,
	): Int

	@Query(
		"""
		SELECT f FROM GarageFeedback f
		WHERE f.garage.id = :garageId AND f.createdAt >= :since
		""",
	)
	fun findForGarageSince(
		@Param("garageId") garageId: Long,
		@Param("since") since: Instant,
	): List<GarageFeedback>

	@Query(
		"""
		SELECT AVG(f.overallRating), COUNT(f) FROM GarageFeedback f
		WHERE f.createdAt >= :since
		""",
	)
	fun averageRatingSince(@Param("since") since: Instant): List<Any>

	@Query(
		"""
		SELECT AVG(f.qualityRating), AVG(f.timelinessRating), AVG(f.priceFairnessRating), AVG(f.staffCourtesyRating)
		FROM GarageFeedback f
		WHERE f.createdAt >= :since
		""",
	)
	fun dimensionAveragesSince(@Param("since") since: Instant): List<Any>
}

fun GarageFeedbackRepository.ratingFor(garageId: Long): Pair<java.math.BigDecimal, Long> =
	ratingSummaryForGarage(garageId).firstOrNull()?.let {
		val row = it as Array<*>
		val sum = row[0] as? java.math.BigDecimal ?: java.math.BigDecimal.ZERO
		val count = (row[1] as? Number)?.toLong() ?: 0L
		sum to count
	} ?: (java.math.BigDecimal.ZERO to 0L)

fun GarageFeedbackRepository.averageSince(since: Instant): Double? {
	val row = averageRatingSince(since).firstOrNull() as? Array<*> ?: return null
	val avg = row[0] as? Number ?: return null
	return avg.toDouble()
}
