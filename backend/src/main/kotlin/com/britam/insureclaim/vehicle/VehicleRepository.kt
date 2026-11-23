package com.britam.insureclaim.vehicle

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.query.Param
import java.util.Optional

interface VehicleRepository : JpaRepository<Vehicle, Long> {

	@EntityGraph(attributePaths = ["owner"])
	override fun findAll(pageable: Pageable): Page<Vehicle>

	fun findByOwnerId(ownerId: Long): List<Vehicle>

	fun findByRegistrationNumberIgnoreCase(registrationNumber: String): Optional<Vehicle>

	fun existsByRegistrationNumberIgnoreCase(registrationNumber: String): Boolean

	@Query("SELECT v FROM Vehicle v WHERE v.owner.id = :ownerId AND v.id = :vehicleId")
	fun findOwnedBy(
		@Param("ownerId") ownerId: Long,
		@Param("vehicleId") vehicleId: Long,
	): Optional<Vehicle>
}
