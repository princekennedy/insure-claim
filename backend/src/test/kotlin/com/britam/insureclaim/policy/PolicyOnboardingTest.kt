package com.britam.insureclaim.policy

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimService
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.claim.UpdateClaimStatusRequest
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.role.Role
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserRepository
import com.britam.insureclaim.vehicle.Vehicle
import com.britam.insureclaim.vehicle.VehicleRepository
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = [
	"spring.datasource.url=jdbc:h2:mem:policy-onboarding;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.flyway.enabled=false",
	"insureclaim.seed.enabled=false",
])
@Transactional
class PolicyOnboardingTest(
	@Autowired private val users: UserRepository,
	@Autowired private val vehicles: VehicleRepository,
	@Autowired private val policies: PolicyRepository,
	@Autowired private val claims: ClaimRepository,
	@Autowired private val policyService: PolicyService,
	@Autowired private val claimService: ClaimService,
) {
	@Test
	fun `policy without insured value blocks approval until value is set`() {
		val customer = users.save(User(email = "policy-customer@example.com", fullName = "Customer", role = Role.CUSTOMER.code))
		val staff = users.save(User(email = "policy-staff@example.com", fullName = "Staff", role = Role.AGENT.code))
		val vehicle = vehicles.save(Vehicle(owner = customer, registrationNumber = "POL-VEHICLE", make = "A", model = "B", year = 2020))
		val created = policyService.create(CreatePolicyRequest(
			customerId = customer.id, vehicleId = vehicle.id, policyNumber = "POL-123",
			startDate = LocalDate.now(), endDate = LocalDate.now().plusYears(1),
			premiumAmount = BigDecimal("100.00"), productCode = "MOTOR_THIRD_PARTY",
		))
		assertNull(created.sumInsured)
		assertEquals("MOTOR_THIRD_PARTY", created.productCode)
		val policy = policies.findById(created.id).orElseThrow()
		val claim = claims.save(Claim(
			claimNumber = "CLM-123", policy = policy, customer = customer, vehicle = vehicle,
			status = ClaimStatus.UNDER_REVIEW, estimatedAmount = BigDecimal("500.00"),
		))
		val request = UpdateClaimStatusRequest(status = ClaimStatus.APPROVED)
		val missing = assertFailsWith<BusinessRuleException> {
			claimService.transition(claim, ClaimStatus.APPROVED, request, staff)
		}
		assertEquals("POLICY_SUM_INSURED_REQUIRED", missing.code)
		assertNull(claim.approvedAmount)

		policyService.update(created.id, UpdatePolicyRequest(sumInsured = BigDecimal("400.00")))
		val exceeded = assertFailsWith<BusinessRuleException> {
			claimService.transition(claim, ClaimStatus.APPROVED, request, staff)
		}
		assertEquals("APPROVED_AMOUNT_EXCEEDS_SUM_INSURED", exceeded.code)

		policyService.update(created.id, UpdatePolicyRequest(sumInsured = BigDecimal("600.00")))
		assertEquals(ClaimStatus.APPROVED, claimService.transition(claim, ClaimStatus.APPROVED, request, staff).status)
	}
}
