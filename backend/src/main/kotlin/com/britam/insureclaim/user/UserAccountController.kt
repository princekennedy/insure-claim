package com.britam.insureclaim.user

import com.britam.insureclaim.common.Auditable
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
	@Auditable(action = "PROFILE_UPDATE", description = "Updated own profile", entityType = "USER")
	fun updateProfile(@Valid @RequestBody request: UpdateProfileRequest): UserResponse =
		UserResponse.from(accountService.updateProfile(currentUser.requireId(), request))

	@Operation(summary = "List vehicles registered to the signed-in customer")
	@GetMapping("/vehicles")
	fun listVehicles(): List<VehicleResponse> = accountService.listVehicles(currentUser.requireId())

	@Operation(summary = "Register a vehicle")
	@PostMapping("/vehicles")
	@Auditable(action = "VEHICLE_CREATE", description = "Registered a vehicle", entityType = "VEHICLE", entityIdFromResponse = true)
	fun addVehicle(@Valid @RequestBody request: VehicleRequest): ResponseEntity<VehicleResponse> =
		ResponseEntity.status(HttpStatus.CREATED).body(accountService.addVehicle(currentUser.requireId(), request))

	@Operation(summary = "Fetch a vehicle the signed-in customer owns")
	@GetMapping("/vehicles/{vehicleId}")
	fun getVehicle(@PathVariable vehicleId: Long): VehicleResponse =
		accountService.getVehicle(currentUser.requireId(), vehicleId)

	@Operation(summary = "Update a vehicle")
	@PutMapping("/vehicles/{vehicleId}")
	@Auditable(action = "VEHICLE_UPDATE", description = "Updated a vehicle", entityType = "VEHICLE")
	fun updateVehicle(
		@PathVariable vehicleId: Long,
		@Valid @RequestBody request: VehicleRequest,
	): VehicleResponse = accountService.updateVehicle(currentUser.requireId(), vehicleId, request)

	@Operation(summary = "Remove a vehicle that has no active policy")
	@DeleteMapping("/vehicles/{vehicleId}")
	@Auditable(action = "VEHICLE_DELETE", description = "Removed a vehicle", entityType = "VEHICLE")
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
		@Parameter(description = "Filter by role code")
		@RequestParam(required = false) query: String? = null,
		@RequestParam(required = false) role: String? = null,
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

	@Operation(summary = "Fetch one user")
	@GetMapping("/{userId}")
	fun get(@PathVariable userId: Long): UserResponse = UserResponse.from(authService.findById(userId))

	@Operation(summary = "Create an account (platform admin only)")
	@PostMapping
	@Auditable(action = "USER_CREATE", description = "Created a user account", entityType = "USER")
	fun create(@Valid @RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> =
		ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(authService.createUser(request)))

	@Operation(summary = "Edit an account (insurer admin or platform admin only)")
	@PutMapping("/{userId}")
	@Auditable(action = "USER_UPDATE", description = "Updated a user account", entityType = "USER")
	fun update(
		@PathVariable userId: Long,
		@Valid @RequestBody request: UpdateUserRequest,
	): UserResponse = UserResponse.from(authService.updateUser(userId, request, currentUser.requireId()))

	@Operation(summary = "Delete an account with no claims (platform admin only)")
	@DeleteMapping("/{userId}")
	@Auditable(action = "USER_DELETE", description = "Deleted a user account", entityType = "USER")
	fun delete(@PathVariable userId: Long): ResponseEntity<Void> {
		authService.deleteUser(userId, currentUser.requireId())
		return ResponseEntity.noContent().build()
	}

	@Operation(summary = "Change a user's role (insurer admin or platform admin only)")
	@PutMapping("/{userId}/role")
	@Auditable(action = "USER_ROLE_CHANGE", description = "Changed a user's role", entityType = "USER")
	fun changeRole(@PathVariable userId: Long, @RequestParam role: String): UserResponse {
		authService.updateRole(userId, role, currentUser.requireId())
		val updated = authService.findById(userId)
		return UserResponse.from(updated)
	}

	@Operation(summary = "Enable or disable an account")
	@PutMapping("/{userId}/enabled")
	@Auditable(action = "USER_ENABLED_TOGGLE", description = "Enabled or disabled an account", entityType = "USER")
	fun setEnabled(@PathVariable userId: Long, @RequestParam enabled: Boolean): ResponseEntity<Void> {
		authService.setEnabled(userId, enabled, currentUser.requireId())
		return ResponseEntity.noContent().build()
	}
}
