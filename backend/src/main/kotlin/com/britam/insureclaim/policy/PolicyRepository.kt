package com.britam.insureclaim.policy

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.Optional

interface PolicyRepository : JpaRepository<Policy, Long> {

	fun findByPolicyNumber(policyNumber: String): Optional<Policy>

	/** Staff search: cover, policyholder or vehicle registration. */
	@Query(
		"""
		SELECT p FROM Policy p
		JOIN FETCH p.vehicle
		JOIN FETCH p.customer
		WHERE (:customerId IS NULL OR p.customer.id = :customerId)
			AND (:status IS NULL OR p.status = :status)
			AND (
				LOWER(p.policyNumber) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%'))
				OR LOWER(p.vehicle.registrationNumber) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%'))
				OR LOWER(p.customer.email) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%'))
				OR LOWER(p.customer.fullName) LIKE LOWER(CONCAT('%', COALESCE(:query, ''), '%'))
			)
		ORDER BY p.endDate DESC
		""",
	)
	fun search(
		@Param("customerId") customerId: Long?,
		@Param("status") status: PolicyStatus?,
		@Param("query") query: String?,
		pageable: Pageable,
	): Page<Policy>

	@Query(
		"""
		SELECT p FROM Policy p
		JOIN FETCH p.vehicle
		WHERE p.customer.id = :customerId
		ORDER BY p.endDate DESC
		""",
	)
	fun findByCustomerIdWithVehicle(@Param("customerId") customerId: Long): List<Policy>

	@Query(
		"""
		SELECT p FROM Policy p
		JOIN FETCH p.vehicle
		WHERE p.id = :policyId
		""",
	)
	fun findByIdWithVehicle(@Param("policyId") policyId: Long): Optional<Policy>

	@Query(
		"""
		SELECT p FROM Policy p
		JOIN FETCH p.vehicle
		WHERE p.customer.id = :customerId AND p.vehicle.id = :vehicleId
		""",
	)
	fun findByCustomerAndVehicle(
		@Param("customerId") customerId: Long,
		@Param("vehicleId") vehicleId: Long,
	): Optional<Policy>

	@Query(
		"""
		SELECT p FROM Policy p
		WHERE p.status = com.britam.insureclaim.policy.PolicyStatus.ACTIVE
			AND p.endDate < :today
		""",
	)
	fun findActiveButExpired(@Param("today") today: LocalDate): List<Policy>
}
