package com.britam.insureclaim.vehicle

import com.britam.insureclaim.common.Auditable
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.user.UserAccountService
import com.britam.insureclaim.user.VehicleRequest
import com.britam.insureclaim.user.VehicleResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class StaffVehicleResponse(
	val vehicle: VehicleResponse,
	val customerId: Long,
	val customerName: String,
	val customerEmail: String,
)

@RestController
@RequestMapping("/api/v1/vehicles")
@PreAuthorize("hasAnyRole('AGENT','INSURER_ADMIN','ADMIN','STAFF')")
class VehicleAdminController(
	private val vehicleRepository: VehicleRepository,
	private val accountService: UserAccountService,
) {
	@GetMapping
	@Transactional(readOnly = true)
	fun list(
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "20") size: Int,
	): PageResponse<StaffVehicleResponse> {
		val vehicles = vehicleRepository.findAll(
			PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by("registrationNumber")),
		)
		return PageResponse.map(vehicles) {
			StaffVehicleResponse(VehicleResponse.from(it), it.owner.id!!, it.owner.fullName, it.owner.email)
		}
	}

	@PostMapping("/customers/{customerId}")
	@Auditable(action = "VEHICLE_CREATE", description = "Registered a vehicle for a customer", entityType = "VEHICLE", entityIdFromResponse = true)
	fun create(
		@PathVariable customerId: Long,
		@Valid @RequestBody request: VehicleRequest,
	): ResponseEntity<VehicleResponse> =
		ResponseEntity.status(HttpStatus.CREATED).body(accountService.addVehicleForCustomer(customerId, request))
}
