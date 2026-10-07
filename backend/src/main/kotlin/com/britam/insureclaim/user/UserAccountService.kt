package com.britam.insureclaim.user

import com.britam.insureclaim.chatbot.ConcernStatus
import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.chatbot.SupportConcernRepository
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.kyc.KycStatus
import com.britam.insureclaim.kyc.KycVerificationRepository
import com.britam.insureclaim.policy.Policy
import com.britam.insureclaim.policy.PolicyRepository
import com.britam.insureclaim.policy.PolicyStatus
import com.britam.insureclaim.vehicle.Vehicle
import com.britam.insureclaim.vehicle.VehicleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
@Transactional
class UserAccountService(
	private val userRepository: UserRepository,
	private val vehicleRepository: VehicleRepository,
	private val policyRepository: PolicyRepository,
	private val claimRepository: ClaimRepository,
	private val kycRepository: KycVerificationRepository,
	private val supportConcernRepository: SupportConcernRepository,
) {
	private val maxVehiclesPerCustomer = 10

	fun updateProfile(userId: Long, request: UpdateProfileRequest): User {
		val user = userRepository.findById(userId).orElseThrow { NotFoundException("User", userId) }
		request.fullName?.let { user.fullName = it.trim() }
		request.phone?.let { user.phone = it.trim() }
		request.nic?.let { user.nic = it.trim().uppercase() }
		return userRepository.save(user)
	}

	fun listVehicles(userId: Long): List<VehicleResponse> =
		vehicleRepository.findByOwnerId(userId)
			.sortedBy { it.registrationNumber }
			.map(VehicleResponse::from)

	fun addVehicle(userId: Long, request: VehicleRequest): VehicleResponse {
		val user = userRepository.findById(userId).orElseThrow { NotFoundException("User", userId) }
		val registration = request.registrationNumber.trim().uppercase()
		if (vehicleRepository.existsByRegistrationNumberIgnoreCase(registration)) {
			throw BusinessRuleException("Vehicle $registration is already registered", "DUPLICATE_REGISTRATION")
		}
		if (vehicleRepository.findByOwnerId(userId).size >= maxVehiclesPerCustomer) {
			throw BusinessRuleException(
				"You can register at most $maxVehiclesPerCustomer vehicles",
				"VEHICLE_LIMIT_REACHED",
			)
		}
		val vehicle = vehicleRepository.save(
			Vehicle(
				owner = user,
				registrationNumber = registration,
				make = request.make.trim(),
				model = request.model.trim(),
				year = request.year,
				color = request.color?.trim(),
				chassisNumber = request.chassisNumber?.trim()?.uppercase(),
				engineNumber = request.engineNumber?.trim()?.uppercase(),
			),
		)
		return VehicleResponse.from(vehicle)
	}

	fun getVehicle(userId: Long, vehicleId: Long): VehicleResponse =
		VehicleResponse.from(requireReadableVehicle(userId, vehicleId))

	fun updateVehicle(userId: Long, vehicleId: Long, request: VehicleRequest): VehicleResponse {
		val vehicle = vehicleRepository.findById(vehicleId).orElseThrow { NotFoundException("Vehicle", vehicleId) }
		ensureOwnership(userId, vehicle.owner.id)

		val registration = request.registrationNumber.trim().uppercase()
		if (!registration.equals(vehicle.registrationNumber, ignoreCase = true) &&
			vehicleRepository.existsByRegistrationNumberIgnoreCase(registration)
		) {
			throw BusinessRuleException("Vehicle $registration is already registered", "DUPLICATE_REGISTRATION")
		}
		vehicle.registrationNumber = registration
		vehicle.make = request.make.trim()
		vehicle.model = request.model.trim()
		vehicle.year = request.year
		vehicle.color = request.color?.trim()
		vehicle.chassisNumber = request.chassisNumber?.trim()?.uppercase()
		vehicle.engineNumber = request.engineNumber?.trim()?.uppercase()
		return VehicleResponse.from(vehicleRepository.save(vehicle))
	}

	fun deleteVehicle(userId: Long, vehicleId: Long) {
		val vehicle = vehicleRepository.findById(vehicleId).orElseThrow { NotFoundException("Vehicle", vehicleId) }
		ensureOwnership(userId, vehicle.owner.id)

		val policy = policyRepository.findByCustomerAndVehicle(userId, vehicleId).orElse(null)
		if (policy != null && policy.status == PolicyStatus.ACTIVE) {
			throw BusinessRuleException(
				"Cannot remove a vehicle that still has an active policy",
				"VEHICLE_HAS_ACTIVE_POLICY",
			)
		}
		vehicleRepository.delete(vehicle)
	}

	@Transactional(readOnly = true)
	fun listPolicies(userId: Long): List<PolicyResponse> {
		val today = LocalDate.now()
		return policyRepository.findByCustomerIdWithVehicle(userId)
			.sortedByDescending { it.endDate }
			.map { policy ->
				policy.status = PolicyStatus.effective(policy.status, policy.endDate, today)
				PolicyResponse.from(policy)
			}
	}

	@Transactional(readOnly = true)
	fun getPolicy(userId: Long, policyId: Long): PolicyResponse {
		val policy = policyRepository.findByIdWithVehicle(policyId)
			.orElseThrow { NotFoundException("Policy", policyId) }
		ensureOwnership(userId, policy.customer.id)
		return PolicyResponse.from(policy)
	}

	@Transactional(readOnly = true)
	fun customerSummary(userId: Long): CustomerSummary {
		val today = LocalDate.now()

		val activePolicy = policyRepository.findByCustomerIdWithVehicle(userId)
			.firstOrNull { PolicyStatus.effective(it.status, it.endDate, today) == PolicyStatus.ACTIVE }
			?.let(PolicyResponse::from)

		return CustomerSummary(
			totalClaims = claimRepository.countByCustomerId(userId),
			openClaims = claimRepository.countByCustomerIdAndStatusIn(userId, ClaimStatus.entries.filter { it.isOpen }),
			settledClaims = claimRepository.countByCustomerIdAndStatusIn(userId, listOf(ClaimStatus.SETTLED)),
			rejectedClaims = claimRepository.countByCustomerIdAndStatusIn(userId, listOf(ClaimStatus.REJECTED)),
			totalClaimedAmount = claimRepository.sumEstimatedByCustomer(userId) ?: BigDecimal.ZERO,
			totalSettledAmount = claimRepository.sumSettledByCustomer(userId, ClaimStatus.SETTLED) ?: BigDecimal.ZERO,
			activePolicy = activePolicy,
			kycVerified = kycRepository.existsByUserIdAndStatus(userId, KycStatus.VERIFIED),
			unreadConcerns = supportConcernRepository
				.findByStatusOrderByCreatedAtDesc(ConcernStatus.OPEN, PageRequest.of(0, 1))
				.totalElements,
		)
	}

	private fun requireReadableVehicle(userId: Long, vehicleId: Long): Vehicle {
		val vehicle = vehicleRepository.findById(vehicleId)
			.orElseThrow { NotFoundException("Vehicle", vehicleId) }
		ensureOwnership(userId, vehicle.owner.id)
		return vehicle
	}

	private fun ensureOwnership(actingUserId: Long, ownerId: Long?) {
		if (actingUserId == ownerId) return
		val acting = userRepository.findById(actingUserId).orElseThrow { NotFoundException("User", actingUserId) }
		if (!acting.isStaff()) {
			throw ForbiddenException("This record belongs to another customer")
		}
	}

	fun requirePolicyForVehicle(customerId: Long, vehicleId: Long): Policy =
		policyRepository.findByCustomerAndVehicle(customerId, vehicleId)
			.orElseThrow {
				BusinessRuleException(
					"No policy is registered for this vehicle. Contact your insurer to add one.",
					"NO_POLICY_FOR_VEHICLE",
				)
			}

	fun requireUser(userId: Long): User =
		userRepository.findById(userId).orElseThrow { NotFoundException("User", userId) }

	fun requireCustomer(customerId: Long): User = requireUser(customerId)

	fun isStaff(userId: Long): Boolean =
		userRepository.findById(userId).map { it.isStaff() }.orElse(false)
}
