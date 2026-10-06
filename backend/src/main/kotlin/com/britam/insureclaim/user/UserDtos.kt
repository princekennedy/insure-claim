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
	val role: Role,
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
			role = user.role,
			enabled = user.enabled,
			initials = user.initials(),
			lastLoginAt = user.lastLoginAt,
		)
	}
}

// -------------------------------------------------------------- profile ----

@Schema(name = "UpdateProfileRequest")
data class UpdateProfileRequest(
	@field:Size(min = 2, max = 160) val fullName: String? = null,
	@field:Pattern(regexp = "^\\+?[0-9 ()-]{7,32}$", message = "must be a valid phone number")
	val phone: String? = null,
	@field:Size(max = 32) val nic: String? = null,
)

// ------------------------------------------------------------- vehicles ----

@Schema(name = "VehicleRequest")
data class VehicleRequest(
	@field:NotBlank @field:Size(max = 32) val registrationNumber: String,
	@field:NotBlank @field:Size(max = 80) val make: String,
	@field:NotBlank @field:Size(max = 80) val model: String,
	@field:jakarta.validation.constraints.Min(1900)
	@jakarta.validation.constraints.Max(2100)
	val year: Int,
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
		fun from(v: Vehicle): VehicleResponse = VehicleResponse(
			id = v.id ?: 0L,
			registrationNumber = v.registrationNumber,
			make = v.make,
			model = v.model,
			year = v.year,
			color = v.color,
			chassisNumber = v.chassisNumber,
			engineNumber = v.engineNumber,
		)
	}
}

// -------------------------------------------------------------- policies ----

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
		fun from(p: Policy): PolicyResponse = PolicyResponse(
			id = p.id ?: 0L,
			policyNumber = p.policyNumber,
			insurerName = p.insurerName,
			productCode = p.productCode,
			startDate = p.startDate,
			endDate = p.endDate,
			premiumAmount = p.premiumAmount,
			sumInsured = p.sumInsured,
			excessAmount = p.excessAmount,
			status = p.status.name,
			vehicle = VehicleResponse.from(p.vehicle),
			isCurrentlyValid = p.isCurrentlyValid(),
		)
	}
}

@Schema(name = "PageResponsePolicy", description = "Page of policies")
data class PolicyPageResponse(
	val content: List<PolicyResponse>,
	val page: Int,
	val size: Int,
	val totalElements: Long,
	val totalPages: Int,
)

// ------------------------------------------------------- dashboard cards ----

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

@Schema(name = "ClaimStatusCount", description = "Number of claims in a given status")
data class ClaimStatusCount(
	val status: ClaimStatus,
	val count: Long,
)
