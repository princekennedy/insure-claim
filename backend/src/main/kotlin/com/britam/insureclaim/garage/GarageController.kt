package com.britam.insureclaim.garage

import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.feedback.GarageFeedbackService
import com.britam.insureclaim.security.CurrentUserResolver
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Garages", description = "Panel garage directory and repair progress")
@RestController
@RequestMapping("/api/v1/garages")
class GarageController(
	private val garageService: GarageService,
	private val feedbackService: GarageFeedbackService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(
		summary = "Browse panel garages",
		description = "Only garages that are active and permitted to take work are returned.",
	)
	@GetMapping
	fun search(
		@Parameter(description = "Filter by city, case-insensitive") @RequestParam(required = false) city: String? = null,
		@Parameter(description = "Match garage name or city") @RequestParam(required = false) query: String? = null,
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "20") size: Int = 20,
	): PageResponse<GarageResponse> = garageService.searchGarages(city, query, page, size)

	@Operation(summary = "Garage profile with rating and turnaround history")
	@GetMapping("/{garageId}")
	fun get(@PathVariable garageId: Long): GarageResponse = garageService.getGarage(garageId)

	@Operation(
		summary = "Ratings left for a garage",
		description = "Comment text is only returned to insurer staff.",
	)
	@GetMapping("/{garageId}/feedback")
	fun feedback(
		@PathVariable garageId: Long,
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "20") size: Int = 20,
	): PageResponse<GarageFeedbackResponse> =
		feedbackService.forGarage(garageId, currentUser.requireRole(), page, size)
}

@Tag(name = "Repair jobs", description = "Garage assignment and repair progress")
@RestController
@RequestMapping("/api/v1/repair-jobs")
@PreAuthorize("hasAnyRole('AGENT', 'INSURER_ADMIN', 'ADMIN')")
class RepairJobController(
	private val garageService: GarageService,
	private val accountService: UserAccountService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(summary = "List repair jobs, optionally for one garage")
	@GetMapping
	fun list(
		@RequestParam(required = false) garageId: Long? = null,
		@Parameter(description = "Defaults to every job still in progress") @RequestParam(required = false) status: RepairJobStatus? = null,
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "20") size: Int = 20,
	): PageResponse<RepairJobResponse> =
		garageService.listJobs(garageId, status, actor(), page, size)

	@Operation(summary = "One repair job with claim and garage detail")
	@GetMapping("/{jobId}")
	fun get(@PathVariable jobId: Long): RepairJobResponse = garageService.getJob(jobId, actor())

	@Operation(summary = "Move a repair to its next stage")
	@PatchMapping("/{jobId}")
	fun update(
		@PathVariable jobId: Long,
		@Valid @RequestBody request: UpdateRepairJobRequest,
	): RepairJobResponse = garageService.updateJob(jobId, request, actor())

	@Operation(summary = "Cancel a repair that is no longer going ahead")
	@DeleteMapping("/{jobId}")
	fun cancel(
		@PathVariable jobId: Long,
		@Parameter(description = "Why the repair was cancelled") @RequestParam(required = false) reason: String? = null,
	): RepairJobResponse = garageService.cancelJob(jobId, reason, actor())

	private fun actor(): User = accountService.requireUser(currentUser.requireId())
}

@Tag(name = "Garage assignment", description = "Routing an approved claim to a panel garage")
@RestController
@RequestMapping("/api/v1/claims")
@PreAuthorize("hasAnyRole('AGENT', 'INSURER_ADMIN', 'ADMIN')")
class GarageAssignmentController(
	private val garageService: GarageService,
	private val accountService: UserAccountService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(
		summary = "Assign a panel garage to an approved claim",
		description = "Creates the repair job and moves the claim to Garage assigned in one step.",
	)
	@PutMapping("/{claimId}/garage")
	fun assign(
		@PathVariable claimId: Long,
		@Valid @RequestBody request: AssignGarageRequest,
	): RepairJobResponse = garageService.assignGarage(claimId, request, actor())

	private fun actor(): User = accountService.requireUser(currentUser.requireId())
}