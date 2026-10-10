package com.britam.insureclaim.user

import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.policy.Policy
import com.britam.insureclaim.vehicle.Vehicle
import com.britam.insureclaim.role.roleNameFor
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

@Schema(name = "LogoutRequest", description = "Optional refresh token to revoke alongside the session")
data class LogoutRequest(val refreshToken: String? = null)

@Schema(name = "ChangePasswordRequest")
data class ChangePasswordRequest(
	@field:NotBlank val currentPassword: String,
	@field:NotBlank @field:Size(min = 10, max = 72) val newPassword: String,
)

@Schema(name = "ForgotPasswordRequest")
data class ForgotPasswordRequest(
	@field:Email @field:NotBlank val email: String,
)

@Schema(name = "ResetPasswordRequest")
data class ResetPasswordRequest(
	@field:NotBlank val token: String,
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
	val nic: String?,
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
			nic = user.nic,
			role = user.role,
			roleName = roleNameFor(user.role) ?: user.role,
			enabled = user.enabled,
			initials = user.initials(),
			lastLoginAt = user.lastLoginAt,
		)
	}
}

@Schema(name = "UpdateProfileRequest")
data class UpdateProfileRequest(
	@field:Size(min = 2, max = 160) val fullName: String? = null,
	@field:Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$", message = "must be a valid phone number")
	val phone: String? = null,
	@field:Size(max = 32) val nic: String? = null,
)

@Schema(name = "CreateUserRequest", description = "Admin-created account")
data class CreateUserRequest(
	@field:Email @field:NotBlank val email: String,
	@field:NotBlank @field:Size(min = 10, max = 72) val password: String,
	@field:NotBlank @field:Size(min = 2, max = 160) val fullName: String,
	@field:Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$", message = "must be a valid phone number")
	val phone: String? = null,
	@field:Size(max = 32) val nic: String? = null,
	@field:NotBlank @field:Size(max = 32) val role: String,
)

@Schema(
	name = "UpdateUserRequest",
	description = "Edit an account. fullName/phone/nic replace the stored values; role/enabled/password are left unchanged when omitted.",
)
data class UpdateUserRequest(
	@field:NotBlank @field:Size(min = 2, max = 160) val fullName: String,
	@field:Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$", message = "must be a valid phone number")
	val phone: String? = null,
	@field:Size(max = 32) val nic: String? = null,
	@field:Size(max = 32) val role: String? = null,
	val enabled: Boolean? = null,
	@field:Size(min = 10, max = 72) val password: String? = null,
)

@Schema(name = "VehicleRequest")
data class VehicleRequest(
	@field:NotBlank @field:Size(max = 32) val registrationNumber: String,
	@field:NotBlank @field:Size(max = 80) val make: String,
	@field:NotBlank @field:Size(max = 80) val model: String,
	@field:Min(1900) @field:Max(2100) val year: Int,
	@field:Size(max = 40) val color: String? = null,
	@field:Size(max = 64) val chassisNumber: String? = null,
	@field:Size(max = 64) val engineNumber: String? = null,
)

@Schema(name = "VehicleResponse")
data class VehicleResponse(
	val id: Long,
	val registrationNumber: String,
	val make: String,
	val model: String,
	val year: Int,
	val color: String?,
	val chassisNumber: String?,
	val engineNumber: String?,
) {
	companion object {
		fun from(vehicle: Vehicle): VehicleResponse = VehicleResponse(
			id = vehicle.id ?: 0L,
			registrationNumber = vehicle.registrationNumber,
			make = vehicle.make,
			model = vehicle.model,
			year = vehicle.year,
			color = vehicle.color,
			chassisNumber = vehicle.chassisNumber,
			engineNumber = vehicle.engineNumber,
		)
	}
}

@Schema(name = "PolicyResponse")
data class PolicyResponse(
	val id: Long,
	val policyNumber: String,
	val insurerName: String,
	val productCode: String,
	val startDate: LocalDate,
	val endDate: LocalDate,
	val premiumAmount: BigDecimal,
	val sumInsured: BigDecimal,
	val excessAmount: BigDecimal,
	val status: String,
	val vehicle: VehicleResponse,
	@param:Schema(description = "True when the policy window covers today")
	val isCurrentlyValid: Boolean,
) {
	companion object {
		fun from(policy: Policy): PolicyResponse = PolicyResponse(
			id = policy.id ?: 0L,
			policyNumber = policy.policyNumber,
			insurerName = policy.insurerName,
			productCode = policy.productCode,
			startDate = policy.startDate,
			endDate = policy.endDate,
			premiumAmount = policy.premiumAmount,
			sumInsured = policy.sumInsured,
			excessAmount = policy.excessAmount,
			status = policy.status.name,
			vehicle = VehicleResponse.from(policy.vehicle),
			isCurrentlyValid = policy.isCurrentlyValid(),
		)
	}
}

@Schema(name = "CustomerSummary")
data class CustomerSummary(
	val totalClaims: Long,
	val openClaims: Long,
	val settledClaims: Long,
	val rejectedClaims: Long,
	val totalClaimedAmount: BigDecimal,
	val totalSettledAmount: BigDecimal,
	val activePolicy: PolicyResponse?,
	val kycVerified: Boolean,
	val unreadConcerns: Long,
)

@Schema(name = "ClaimStatusCount")
data class ClaimStatusCount(
	val status: ClaimStatus,
	val count: Long,
)