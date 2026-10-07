package com.britam.insureclaim.user

import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.security.CurrentUserResolver
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Account", description = "Profile, registered vehicles and policies")
@RestController
@RequestMapping("/api/v1/me")
class UserAccountController(
	private val accountService: UserAccountService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(summary = "Current account summary shown on the customer dashboard")
	@GetMapping("/summary")
	fun summary(): CustomerSummary = accountService.customerSummary(currentUser.requireId())

	@Operation(summary = "Update the signed-in user's profile")
	@PutMapping("/profile")
	fun updateProfile(@Valid @RequestBody request: UpdateProfileRequest): UserResponse =
		UserResponse.from(accountService.updateProfile(currentUser.requireId(), request))

	@Operation(summary = "List vehicles registered to the signed-in customer")
	@GetMapping("/vehicles")
	fun listVehicles(): List<VehicleResponse> = accountService.listVehicles(currentUser.requireId())

	@Operation(summary = "Register a vehicle")
	@PostMapping("/vehicles")
	fun addVehicle(@Valid @RequestBody request: VehicleRequest): ResponseEntity<VehicleResponse> =
		ResponseEntity.status(HttpStatus.CREATED).body(accountService.addVehicle(currentUser.requireId(), request))

	@Operation(summary = "Fetch a vehicle the signed-in customer owns")
	@GetMapping("/vehicles/{vehicleId}")
	fun getVehicle(@PathVariable vehicleId: Long): VehicleResponse =
		accountService.getVehicle(currentUser.requireId(), vehicleId)

	@Operation(summary = "Update a vehicle")
	@PutMapping("/vehicles/{vehicleId}")
	fun updateVehicle(
		@PathVariable vehicleId: Long,
		@Valid @RequestBody request: VehicleRequest,
	): VehicleResponse = accountService.updateVehicle(currentUser.requireId(), vehicleId, request)

	@Operation(summary = "Remove a vehicle that has no active policy")
	@DeleteMapping("/vehicles/{vehicleId}")
	fun deleteVehicle(@PathVariable vehicleId: Long): ResponseEntity<Void> {
		accountService.deleteVehicle(currentUser.requireId(), vehicleId)
		return ResponseEntity.noContent().build()
	}

	@Operation(summary = "List the signed-in customer's policies")
	@GetMapping("/policies")
	fun listPolicies(): List<PolicyResponse> = accountService.listPolicies(currentUser.requireId())

	@Operation(summary = "Fetch one policy")
	@GetMapping("/policies/{policyId}")
	fun getPolicy(@PathVariable policyId: Long): PolicyResponse =
		accountService.getPolicy(currentUser.requireId(), policyId)
}

@Tag(name = "Users", description = "Staff-only user administration")
@RestController
@RequestMapping("/api/v1/users")
class UserAdminController(
	private val authService: AuthService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(summary = "Search users (staff only)")
	@GetMapping
	fun search(
		@Parameter(description = "Free-text match on name, email or NIC")
		@RequestParam(required = false) query: String? = null,
		@RequestParam(required = false) role: com.britam.insureclaim.role.Role? = null,
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "20") size: Int = 20,
	): PageResponse<UserSummary> {
		val pageable = PageRequest.of(
			page.coerceAtLeast(0),
			size.coerceIn(1, 100),
			Sort.by("createdAt").descending(),
		)
		val result = authService.search(query, role, pageable)
		return PageResponse(
			content = result.content.map(User::toSummary),
			page = result.number,
			size = result.size,
			totalElements = result.totalElements,
			totalPages = result.totalPages,
			first = result.isFirst,
			last = result.isLast,
			empty = result.isEmpty,
		)
	}

	@Operation(summary = "Change a user's role (insurer admin or platform admin only)")
	@PutMapping("/{userId}/role")
	fun changeRole(@PathVariable userId: Long, @RequestParam role: com.britam.insureclaim.role.Role): UserResponse {
		authService.updateRole(userId, role, currentUser.requireId())
		val updated = authService.findById(userId)
		return UserResponse.from(updated)
	}

	@Operation(summary = "Enable or disable an account")
	@PutMapping("/{userId}/enabled")
	fun setEnabled(@PathVariable userId: Long, @RequestParam enabled: Boolean): ResponseEntity<Void> {
		authService.setEnabled(userId, enabled, currentUser.requireId())
		return ResponseEntity.noContent().build()
	}
}
