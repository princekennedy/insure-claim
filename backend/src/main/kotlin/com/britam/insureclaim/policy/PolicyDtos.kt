package com.britam.insureclaim.policy

import com.britam.insureclaim.user.UserResponse
import com.britam.insureclaim.user.VehicleResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Staff-side cover onboarding. The customer themselves cannot issue cover -
 * there is deliberately no self-service path, so every field here is checked
 * against a real customer and a vehicle they actually own.
 */
@Schema(name = "CreatePolicyRequest")
data class CreatePolicyRequest(
	@Schema(description = "Customer the cover is issued to")
	@field:NotNull
	val customerId: Long? = null,

	@Schema(description = "Vehicle being covered; must belong to the customer")
	@field:NotNull
	val vehicleId: Long? = null,

	@Schema(description = "Left blank to have the portal generate one")
	@field:Size(max = 48)
	val policyNumber: String? = null,

	@field:NotNull
	val startDate: LocalDate? = null,

	@field:NotNull
	val endDate: LocalDate? = null,

	@field:NotNull
	@field:DecimalMin(value = "0.01")
	val premiumAmount: BigDecimal? = null,

	@field:NotNull
	@field:DecimalMin(value = "0.01")
	val sumInsured: BigDecimal? = null,

	@field:DecimalMin(value = "0.00")
	val excessAmount: BigDecimal? = null,

	@field:Size(max = 160)
	val insurerName: String? = null,

	@field:Size(max = 64)
	val productCode: String? = null,

	val status: PolicyStatus? = null,
)

@Schema(name = "UpdatePolicyRequest")
data class UpdatePolicyRequest(
	val endDate: LocalDate? = null,
	val premiumAmount: BigDecimal? = null,
	val sumInsured: BigDecimal? = null,
	val excessAmount: BigDecimal? = null,
	val status: PolicyStatus? = null,
)

@Schema(name = "PolicyAdminResponse", description = "Policy as seen by insurer staff, with the policyholder attached")
data class PolicyAdminResponse(
	val id: Long,
	val policyNumber: String,
	val customer: UserResponse,
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
		fun from(p: Policy): PolicyAdminResponse = PolicyAdminResponse(
			id = p.id ?: 0L,
			policyNumber = p.policyNumber,
			customer = UserResponse.from(p.customer),
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
