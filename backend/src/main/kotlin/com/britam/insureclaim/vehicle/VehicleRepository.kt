package com.britam.insureclaim.vehicle

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface VehicleRepository : JpaRepository<Vehicle, Long> {

	fun findByOwnerId(ownerId: Long): List<Vehicle>

	fun findByRegistrationNumberIgnoreCase(registrationNumber: String): Optional<Vehicle>

	fun existsByRegistrationNumberIgnoreCase(registrationNumber: String): Boolean

	@Query("SELECT v FROM Vehicle v WHERE v.owner.id = :ownerId AND v.id = :vehicleId")
	fun findOwnedBy(
		@Param("ownerId") ownerId: Long,
		@Param("vehicleId") vehicleId: Long,
	): Optional<Vehicle>
}
