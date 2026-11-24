package com.britam.insureclaim.task

import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.role.Role
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ClaimReviewTaskService(
	private val tasks: ClaimReviewTaskRepository,
	private val assignments: ClaimReviewTaskAssignmentRepository,
	private val users: UserRepository,
) {
	companion object {
		private val reviewStatuses = setOf(
			ClaimStatus.SUBMITTED, ClaimStatus.KYC_PENDING, ClaimStatus.UNDER_REVIEW, ClaimStatus.FRAUD_CHECK,
		)
	}

	@Transactional(readOnly = true)
	fun list(actor: User): List<ClaimReviewTaskResponse> {
		requireQueueRole(actor)
		return tasks.findByClaimStatusInOrderByCreatedAtAsc(reviewStatuses).map { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun officers(actor: User): List<OfficerResponse> {
		requireManager(actor)
		return users.findByRoleAndEnabledTrueOrderByFullNameAsc(Role.OFFICER.code)
			.map { OfficerResponse(requireNotNull(it.id), it.fullNameOrEmail()) }
	}

	@Transactional
	fun claim(taskId: Long, actor: User): ClaimReviewTaskResponse {
		if (actor.role != Role.OFFICER.code) throw ForbiddenException("Only an officer can claim a task")
		val task = taskForUpdate(taskId)
		if (task.assignee != null) throw ConflictException("This task is already assigned", "TASK_ALREADY_ASSIGNED")
		return assign(task, actor, actor)
	}

	@Transactional
	fun assign(taskId: Long, officerId: Long, actor: User): ClaimReviewTaskResponse {
		requireManager(actor)
		val task = taskForUpdate(taskId)
		val officer = users.findById(officerId).orElseThrow { NotFoundException("Officer", officerId) }
		if (officer.role != Role.OFFICER.code || !officer.enabled) {
			throw BusinessRuleException("The selected user must be an active officer", "INVALID_OFFICER")
		}
		if (task.assignee?.id == officer.id) throw ConflictException("This officer is already assigned", "TASK_ALREADY_ASSIGNED")
		return assign(task, officer, actor)
	}

	private fun taskForUpdate(taskId: Long): ClaimReviewTask {
		val task = tasks.findForUpdate(taskId).orElseThrow { NotFoundException("Task", taskId) }
		if (task.claim.status !in reviewStatuses) {
			throw ConflictException("This claim no longer needs a review task", "TASK_NO_LONGER_ACTIVE")
		}
		return task
	}

	private fun assign(task: ClaimReviewTask, officer: User, actor: User): ClaimReviewTaskResponse {
		val now = Instant.now()
		task.assignee = officer
		task.assignedBy = actor
		task.assignedAt = now
		tasks.save(task)
		assignments.save(ClaimReviewTaskAssignment(
			task = task,
			officerId = officer.id,
			actorId = actor.id,
			officerLabel = officer.fullNameOrEmail(),
			actorLabel = actor.fullNameOrEmail(),
			assignedAt = now,
		))
		return task.toResponse()
	}

	private fun requireQueueRole(actor: User) {
		if (actor.role != Role.OFFICER.code && actor.role != Role.MANAGER.code) {
			throw ForbiddenException("Only officers and managers can access claim review tasks")
		}
	}

	private fun requireManager(actor: User) {
		if (actor.role != Role.MANAGER.code) throw ForbiddenException("Only a manager can assign tasks")
	}
}

data class OfficerResponse(val id: Long, val fullName: String)

data class ClaimReviewTaskResponse(
	val id: Long,
	val claimId: Long,
	val claimNumber: String,
	val customerName: String,
	val vehicleRegistration: String,
	val claimStatus: ClaimStatus,
	val submittedAt: Instant,
	val assigneeId: Long?,
	val assigneeName: String?,
	val assignedAt: Instant?,
	val assignedByName: String?,
)

private fun ClaimReviewTask.toResponse() = ClaimReviewTaskResponse(
	id = requireNotNull(id),
	claimId = requireNotNull(claim.id),
	claimNumber = claim.claimNumber,
	customerName = claim.customer.fullNameOrEmail(),
	vehicleRegistration = claim.vehicle.registrationNumber,
	claimStatus = claim.status,
	submittedAt = claim.submittedAt,
	assigneeId = assignee?.id,
	assigneeName = assignee?.fullNameOrEmail(),
	assignedAt = assignedAt,
	assignedByName = assignedBy?.fullNameOrEmail(),
)
