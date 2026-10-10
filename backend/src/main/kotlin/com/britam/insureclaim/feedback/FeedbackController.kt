package com.britam.insureclaim.feedback

import com.britam.insureclaim.common.Auditable
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.garage.GarageFeedbackRequest
import com.britam.insureclaim.garage.GarageFeedbackResponse
import com.britam.insureclaim.security.CurrentUserResolver
import com.britam.insureclaim.user.UserAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Feedback", description = "Customer ratings of panel garage work")
@RestController
@RequestMapping("/api/v1")
class FeedbackController(
	private val feedbackService: GarageFeedbackService,
	private val accountService: UserAccountService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(
		summary = "Rate a completed repair",
		description = "One rating per claim. Unlocks once the vehicle has been collected.",
	)
	@PostMapping("/claims/{claimId}/feedback")
@Auditable(action = "FEEDBACK_CREATE", description = "Left repair feedback", entityType = "FEEDBACK", entityIdFromResponse = true)
	fun submit(
		@PathVariable claimId: Long,
		@Valid @RequestBody request: GarageFeedbackRequest,
	): ResponseEntity<GarageFeedbackResponse> {
		val saved = feedbackService.submit(claimId, request, accountService.requireUser(currentUser.requireId()))
		return ResponseEntity.status(HttpStatus.CREATED).body(saved)
	}

	@Operation(summary = "Ratings you have left")
	@GetMapping("/me/feedback")
	fun mine(
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "20") size: Int = 20,
	): PageResponse<GarageFeedbackResponse> = feedbackService.myFeedback(currentUser.requireId(), page, size)
}