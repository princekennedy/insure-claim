package com.britam.insureclaim.task

import com.britam.insureclaim.common.Auditable
import com.britam.insureclaim.security.CurrentUserResolver
import com.britam.insureclaim.user.UserAccountService
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tasks")
@PreAuthorize("hasAnyRole('OFFICER', 'MANAGER', 'ADMIN', 'INSURER_ADMIN')")
class ClaimReviewTaskController(
	private val service: ClaimReviewTaskService,
	private val users: UserAccountService,
	private val currentUser: CurrentUserResolver,
) {
	@GetMapping
	fun list(): List<ClaimReviewTaskResponse> = service.list(actor())

	@GetMapping("/officers")
	fun officers(): List<OfficerResponse> = service.officers(actor())

	@PostMapping("/{taskId}/claim")
	@Auditable(action = "CLAIM_TASK_CLAIM", description = "Claimed a review task", entityType = "CLAIM_TASK")
	fun claim(@PathVariable taskId: Long): ClaimReviewTaskResponse = service.claim(taskId, actor())

	@PutMapping("/{taskId}/assignment")
	@Auditable(action = "CLAIM_TASK_ASSIGN", description = "Assigned a review task", entityType = "CLAIM_TASK")
	fun assign(@PathVariable taskId: Long, @Valid @RequestBody request: AssignTaskRequest): ClaimReviewTaskResponse =
		service.assign(taskId, request.officerId, actor())

	private fun actor() = users.requireUser(currentUser.requireId())
}

data class AssignTaskRequest(@field:Positive val officerId: Long)
