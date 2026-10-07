package com.britam.insureclaim.policy

import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.common.ValidationException
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserRepository
import com.britam.insureclaim.vehicle.VehicleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

/**
 * Cover onboarding for insurer staff.
 *
 * The portal has no self-service way to create a policy, so this is the only
 * path by which a customer can ever become claimable - which is why ownership
 * (does the vehicle belong to that customer?) and the policy window are both
 * enforced here rather than trusted from the request.
 */
@Service
class PolicyService(
	private val policyRepository: PolicyRepository,
	private val userRepository: UserRepository,
	private val vehicleRepository: VehicleRepository,
) {

	@Transactional
	fun create(request: CreatePolicyRequest): PolicyAdminResponse {
		val customer = userRepository.findById(request.customerId!!)
			.orElseThrow { NotFoundException("Customer", request.customerId) }
		if (customer.role?.code != "CUSTOMER") {
			throw BusinessRuleException("Cover can only be issued to a customer account", "POLICY_CUSTOMER_ONLY")
		}

		val vehicle = vehicleRepository.findById(request.vehicleId!!)
			.orElseThrow { NotFoundException("Vehicle", request.vehicleId) }
		if (vehicle.owner.id != customer.id) {
			throw BusinessRuleException(
				"That vehicle is not registered to this customer",
				"VEHICLE_NOT_OWNED_BY_CUSTOMER",
			)
		}

		val startDate = request.startDate!!
		val endDate = request.endDate!!
		if (endDate.isBefore(startDate)) {
			throw ValidationException("The policy end date cannot be before the start date", "POLICY_END_BEFORE_START")
		}
		if (endDate.isBefore(LocalDate.now())) {
			throw BusinessRuleException("The policy end date is already in the past", "POLICY_ALREADY_EXPIRED")
		}

		val status = request.status ?: PolicyStatus.ACTIVE
		if (status == PolicyStatus.ACTIVE && endDate.isBefore(LocalDate.now())) {
			throw BusinessRuleException("An active policy must run until at least today", "POLICY_ALREADY_EXPIRED")
		}

		val policy = Policy().apply {
			policyNumber = resolvePolicyNumber(request.policyNumber)
			this.customer = customer
			this.vehicle = vehicle
			insurerName = request.insurerName?.takeIf { it.isNotBlank() } ?: Policy.DEFAULT_INSURER
			productCode = request.productCode?.takeIf { it.isNotBlank() } ?: Policy.DEFAULT_PRODUCT
			this.startDate = startDate
			this.endDate = endDate
			premiumAmount = request.premiumAmount!!
			sumInsured = request.sumInsured!!
			excessAmount = request.excessAmount ?: java.math.BigDecimal.ZERO
			this.status = status
		}
		return PolicyAdminResponse.from(policyRepository.save(policy))
	}

	@Transactional
	fun update(policyId: Long, request: UpdatePolicyRequest): PolicyAdminResponse {
		val policy = policyRepository.findByIdWithVehicle(policyId)
			.orElseThrow { NotFoundException("Policy", policyId) }

		request.endDate?.let { end ->
			if (end.isBefore(policy.startDate)) {
				throw ValidationException("The policy end date cannot be before the start date", "POLICY_END_BEFORE_START")
			}
			policy.endDate = end
		}
		request.premiumAmount?.let { policy.premiumAmount = it }
		request.sumInsured?.let { policy.sumInsured = it }
		request.excessAmount?.let { policy.excessAmount = it }
		request.status?.let { policy.status = it }

		return PolicyAdminResponse.from(policyRepository.save(policy))
	}

	@Transactional(readOnly = true)
	fun list(
		customerId: Long?,
		status: PolicyStatus?,
		query: String?,
		page: Int,
		size: Int,
	): PageResponse<PolicyAdminResponse> {
		val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
		val source = policyRepository.search(customerId, status, query?.trim()?.takeIf { it.isNotEmpty() }, pageable)
		return PageResponse(
			content = source.content.map { PolicyAdminResponse.from(it) },
			page = source.number,
			size = source.size,
			totalElements = source.totalElements,
			totalPages = source.totalPages,
			first = source.isFirst,
			last = source.isLast,
			empty = source.isEmpty,
		)
	}

	/** A supplied number wins; otherwise one is minted that cannot collide. */
	private fun resolvePolicyNumber(supplied: String?): String {
		val requested = supplied?.trim().orEmpty()
		if (requested.isNotEmpty()) {
			if (policyRepository.findByPolicyNumber(requested).isPresent) {
				throw ConflictException("Policy number $requested is already in use", "POLICY_NUMBER_TAKEN")
			}
			return requested
		}
		var candidate: String
		do {
			candidate = "POL-${LocalDate.now().year}-${UUID.randomUUID().toString().take(8).uppercase()}"
		} while (policyRepository.findByPolicyNumber(candidate).isPresent)
		return candidate
	}
}
