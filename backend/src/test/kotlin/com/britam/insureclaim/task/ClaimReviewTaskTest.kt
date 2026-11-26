package com.britam.insureclaim.task

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimService
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.claim.FileClaimRequest
import com.britam.insureclaim.claim.IncidentType
import com.britam.insureclaim.claim.UpdateClaimStatusRequest
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.policy.Policy
import com.britam.insureclaim.policy.PolicyRepository
import com.britam.insureclaim.role.Role
import com.britam.insureclaim.security.AuthenticatedUser
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserRepository
import com.britam.insureclaim.vehicle.Vehicle
import com.britam.insureclaim.vehicle.VehicleRepository
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = [
	"spring.datasource.url=jdbc:h2:mem:claim-tasks;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.flyway.enabled=false",
	"insureclaim.seed.enabled=false",
])
@Transactional
class ClaimReviewTaskTest(
	@Autowired private val users: UserRepository,
	@Autowired private val vehicles: VehicleRepository,
	@Autowired private val policies: PolicyRepository,
	@Autowired private val claims: ClaimRepository,
	@Autowired private val claimService: ClaimService,
	@Autowired private val tasks: ClaimReviewTaskRepository,
	@Autowired private val assignments: ClaimReviewTaskAssignmentRepository,
	@Autowired private val service: ClaimReviewTaskService,
	@Autowired private val controller: ClaimReviewTaskController,
) {
	@Test
	fun `administrators can view tasks but cannot claim or assign them`() {
		val task = reviewTask()
		val officer = user("officer", Role.OFFICER.code)
		for (role in listOf(Role.ADMIN.code, Role.INSURER_ADMIN.code)) {
			val admin = user(role, role)
			val principal = AuthenticatedUser(requireNotNull(admin.id), admin.email, admin.role, "Admin")
			SecurityContextHolder.getContext().authentication =
				UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
			try {
				assertEquals(task.claim.id, controller.list().single().claimId)
				assertFailsWith<ForbiddenException> { controller.officers() }
				assertFailsWith<ForbiddenException> { controller.claim(requireNotNull(task.id)) }
				assertFailsWith<ForbiddenException> {
					controller.assign(requireNotNull(task.id), AssignTaskRequest(requireNotNull(officer.id)))
				}
			} finally {
				SecurityContextHolder.clearContext()
			}
		}
	}

	@Test
	fun `submitting a claim creates an unassigned task including when KYC is pending`() {
		val customer = user("customer", Role.CUSTOMER.code)
		val vehicle = vehicleAndPolicy(customer)
		val claim = claimService.fileClaim(customer, FileClaimRequest(
			vehicleId = requireNotNull(vehicle.id), incidentType = IncidentType.GLASS,
			incidentDate = LocalDate.now(), description = "Vehicle window was damaged",
			requiresKyc = true,
		))
		val officer = user("officer", Role.OFFICER.code)
		val task = service.list(officer).single()
		assertEquals(ClaimStatus.KYC_PENDING, claim.status)
		assertEquals(claim.id, task.claimId)
		assertNull(task.assigneeId)
	}

	@Test
	fun `submitting an existing draft creates a review task`() {
		val customer = user("customer", Role.CUSTOMER.code)
		val vehicle = vehicleAndPolicy(customer)
		val claim = claims.save(Claim(
			claimNumber = "DRAFT-CLAIM", customer = customer, vehicle = vehicle, policy = policies.findAll().single(),
			status = ClaimStatus.DRAFT,
		))
		claimService.transition(claim, ClaimStatus.SUBMITTED, UpdateClaimStatusRequest(status = ClaimStatus.SUBMITTED), customer)
		val officer = user("officer", Role.OFFICER.code)
		assertEquals(claim.id, service.list(officer).single().claimId)
	}

	@Test
	fun `officers claim only unassigned tasks and managers can reassign with history`() {
		val task = reviewTask()
		val first = user("first", Role.OFFICER.code)
		val second = user("second", Role.OFFICER.code)
		val manager = user("manager", Role.MANAGER.code)
		val id = requireNotNull(task.id)

		assertEquals(first.id, service.claim(id, first).assigneeId)
		assertFailsWith<ConflictException> { service.claim(id, second) }
		assertFailsWith<ForbiddenException> { service.assign(id, requireNotNull(second.id), first) }
		assertEquals(second.id, service.assign(id, requireNotNull(second.id), manager).assigneeId)
		assertEquals(2, assignments.count())
		assertFailsWith<BusinessRuleException> { service.assign(id, requireNotNull(manager.id), manager) }
	}

	@Test
	fun `closed review tasks are not listed or assignable and other roles cannot access queue`() {
		val task = reviewTask()
		val officer = user("officer", Role.OFFICER.code)
		val customer = user("other", Role.CUSTOMER.code)
		assertFailsWith<ForbiddenException> { service.list(customer) }
		task.claim.status = ClaimStatus.REJECTED
		claims.saveAndFlush(task.claim)
		assertEquals(emptyList(), service.list(officer))
		assertFailsWith<ConflictException> { service.claim(requireNotNull(task.id), officer) }
	}

	private fun user(name: String, role: String) =
		users.save(User(email = "$name@example.com", fullName = name, role = role))

	private fun vehicleAndPolicy(customer: User): Vehicle {
		val vehicle = vehicles.save(Vehicle(owner = customer, registrationNumber = "TASK-VEH", make = "A", model = "B", year = 2020))
		policies.save(Policy(
			policyNumber = "TASK-POLICY", customer = customer, vehicle = vehicle,
			startDate = LocalDate.now().minusDays(1), endDate = LocalDate.now().plusYears(1),
		))
		return vehicle
	}

	private fun reviewTask(): ClaimReviewTask {
		val customer = user("customer", Role.CUSTOMER.code)
		val vehicle = vehicleAndPolicy(customer)
		val policy = policies.findAll().single()
		val claim = claims.save(Claim(
			claimNumber = "TASK-CLAIM", customer = customer, vehicle = vehicle, policy = policy,
			status = ClaimStatus.SUBMITTED,
		))
		return tasks.save(ClaimReviewTask(claim = claim))
	}
}
