package com.britam.insureclaim.garage

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.Optional

interface GarageRepository : JpaRepository<Garage, Long> {

	fun findByCode(code: String): Optional<Garage>

	fun findByCityIgnoreCase(city: String): List<Garage>

	@Query(
		"""
		SELECT g FROM Garage g
		WHERE g.active = true
			AND (:city IS NULL OR LOWER(g.city) = LOWER(:city))
			AND (:query IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%'))
				OR LOWER(g.city) LIKE LOWER(CONCAT('%', :query, '%')))
			ORDER BY g.ratingAverage DESC, g.name ASC
		""",
	)
	fun searchActive(
		@Param("city") city: String?,
		@Param("query") query: String?,
		pageable: Pageable,
	): Page<Garage>

	@Query(
		"""
		SELECT g FROM Garage g
		WHERE g.performanceStatus <> :good
		ORDER BY g.performanceStatus DESC, g.ratingAverage ASC
		""",
	)
	fun findNotPerforming(@Param("good") good: GaragePerformanceStatus): List<Garage>

	@Query(
		"""
		SELECT g FROM Garage g
		ORDER BY g.ratingAverage DESC, g.name ASC
		""",
	)
	fun findAllOrderedByRating(): List<Garage>
}

interface RepairJobRepository : JpaRepository<RepairJob, Long> {

	fun findByClaimId(claimId: Long): Optional<RepairJob>

	/** Batch variant so list endpoints do not issue one query per claim. */
	@Query(
		"""
		SELECT j FROM RepairJob j
		JOIN FETCH j.garage
		WHERE j.claim.id IN :claimIds
		""",
	)
	fun findByClaimIds(@Param("claimIds") claimIds: Collection<Long>): List<RepairJob>

	fun findByReferenceCode(referenceCode: String): Optional<RepairJob>

	fun findByGarageIdOrderByAssignedAtDesc(garageId: Long, pageable: Pageable): Page<RepairJob>

	fun countByGarageIdAndStatusNot(garageId: Long, status: RepairJobStatus): Long

	@Query("SELECT j FROM RepairJob j JOIN FETCH j.garage JOIN FETCH j.claim WHERE j.id = :jobId")
	fun findByIdWithDetails(@Param("jobId") jobId: Long): Optional<RepairJob>

	@Query(
		"""
		SELECT j FROM RepairJob j
		WHERE j.garage.id = :garageId AND j.status NOT IN :finishedStatuses
		""",
	)
	fun countActiveForGarage(
		@Param("garageId") garageId: Long,
		@Param("finishedStatuses") finishedStatuses: Collection<RepairJobStatus>,
	): Long

	@Query(
		"""
		SELECT j FROM RepairJob j
		JOIN FETCH j.garage
		WHERE j.startedAt IS NOT NULL AND j.completedAt IS NOT NULL
			AND j.completedAt >= :since
		""",
	)
	fun findCompletedSince(@Param("since") since: Instant): List<RepairJob>

	@Query("SELECT j.status, COUNT(j) FROM RepairJob j GROUP BY j.status")
	fun countGroupedByStatus(): List<Array<Any>>
}
