package com.britam.insureclaim.user

import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.policy.Policy
import com.britam.insureclaim.vehicle.Vehicle
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
// ---------------------------------------------------------------- auth ----

@Schema(name = "RegisterRequest", description = "New policyholder or staff self-registration")
data class RegisterRequest(
	@field:Email @field:NotBlank val email: String,
	@field:NotBlank @field:Size(min = 10, max = 72) val password: String,
	@field:NotBlank @field:Size(min = 2, max = 160) val fullName: String,
	@field:Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$", message = "must be a valid phone number")
	val phone: String? = null,
	@field:Size(max = 32) val nic: String? = null,
)

@Schema(name = "LoginRequest")
data class LoginRequest(
	@field:Email @field:NotBlank val email: String,
	@field:NotBlank val password: String,
)

@Schema(name = "RefreshRequest")
data class RefreshRequest(@field:NotBlank val refreshToken: String)

@Schema(name = "ChangePasswordRequest")
data class ChangePasswordRequest(
	@field:NotBlank val currentPassword: String,
	@field:NotBlank @field:Size(min = 10, max = 72) val newPassword: String,
)

@Schema(name = "TokenResponse")
data class TokenResponse(
	val accessToken: String,
	val refreshToken: String,
	@param:Schema(description = "Access token lifetime in seconds") val expiresIn: Long,
	val tokenType: String = "Bearer",
	val user: UserResponse,
)

@Schema(name = "UserResponse")
data class UserResponse(
	val id: Long,
	val email: String,
	val fullName: String,
	val phone: String?,
	val role: String,
	val roleName: String?,
	val enabled: Boolean,
	val initials: String,
	val lastLoginAt: Instant?,
) {
	companion object {
		fun from(user: User): UserResponse = UserResponse(
			id = user.id ?: 0L,
			email = user.email,
			fullName = user.fullName,
			phone = user.phone,
			role = user.role?.code ?: "CUSTOMER",
			roleName = user.role?.name,
			enabled = user.enabled,
			initials = user.initials(),
			lastLoginAt = user.lastLoginAt,
		)
	}
}