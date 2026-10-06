package com.britam.insureclaim.kyc

import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.security.CurrentUserResolver
import com.britam.insureclaim.user.UserAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.TimeUnit

@Tag(name = "KYC", description = "Digital identity verification required before a claim can be assessed")
@RestController
@RequestMapping("/api/v1/kyc")
class KycController(
	private val kycService: KycService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(
		summary = "Submit identity documents for verification",
		description = "Screened automatically where possible; ambiguous submissions wait for insurer staff.",
	)
	@PostMapping(
		"/verifications",
		consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
		produces = [MediaType.APPLICATION_JSON_VALUE],
	)
	fun submit(
		@Parameter(description = "Identity details as a JSON part")
		@Valid @RequestPart("submission") submission: KycSubmissionRequest,
		@RequestPart(value = "frontImage", required = false) frontImage: MultipartFile?,
		@RequestPart(value = "backImage", required = false) backImage: MultipartFile?,
		@RequestPart(value = "selfie", required = false) selfie: MultipartFile?,
	): ResponseEntity<KycVerificationResponse> {
		val response = kycService.submit(currentUser.requireId(), submission, frontImage, backImage, selfie)
		return ResponseEntity.status(HttpStatus.CREATED).body(response)
	}

	@Operation(summary = "Most recent verification for the signed-in customer")
	@GetMapping("/verifications/current")
	fun current(): ResponseEntity<KycVerificationResponse> =
		kycService.latest(currentUser.requireId())
			?.let { ResponseEntity.ok(it) }
			?: ResponseEntity.noContent().build()

	@Operation(summary = "Verified identities on the signed-in customer's account")
	@GetMapping("/verifications")
	fun history(): List<KycVerificationResponse> = kycService.history(currentUser.requireId())

	@Operation(summary = "Fetch one of the signed-in customer's verifications")
	@GetMapping("/verifications/{verificationId}")
	fun get(@PathVariable verificationId: Long): KycVerificationResponse =
		kycService.getOwn(currentUser.requireId(), verificationId)
}

@Tag(name = "KYC review", description = "Insurer-staff verification queue")
@RestController
@RequestMapping("/api/v1/admin/kyc")
@PreAuthorize("hasAnyRole('AGENT', 'INSURER_ADMIN', 'ADMIN')")
class KycReviewController(
	private val kycService: KycService,
	private val accountService: UserAccountService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(summary = "Submissions waiting for a decision")
	@GetMapping("/verifications")
	fun queue(
		@Parameter(description = "Defaults to PENDING")
		@RequestParam(required = false) status: KycStatus? = null,
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "20") size: Int = 20,
	): PageResponse<KycVerificationResponse> = kycService.queue(status, page, size)

	@Operation(summary = "Approve a submission after manual checks")
	@PostMapping("/verifications/{verificationId}/approve")
	fun approve(@PathVariable verificationId: Long): KycVerificationResponse {
		val reviewer = accountService.requireUser(currentUser.requireId())
		return kycService.decide(verificationId, reviewer, approved = true, reason = null, resubmitAllowed = false)
	}

	@Operation(summary = "Reject a submission, optionally allowing a resubmission")
	@PostMapping("/verifications/{verificationId}/reject")
	fun reject(
		@PathVariable verificationId: Long,
		@Valid @RequestBody request: KycRejectionRequest,
	): KycVerificationResponse {
		val reviewer = accountService.requireUser(currentUser.requireId())
		return kycService.decide(
			verificationId = verificationId,
			reviewer = reviewer,
			approved = false,
			reason = request.reason,
			resubmitAllowed = request.resubmitAllowed,
		)
	}

	@Operation(summary = "Open one document image for review")
	@GetMapping("/verifications/{verificationId}/images/{side}")
	fun image(
		@PathVariable verificationId: Long,
		@Parameter(description = "Which side of the submission to open") @PathVariable side: KycImageSide,
	): ResponseEntity<ByteArray> {
		val reviewer = accountService.requireUser(currentUser.requireId())
		val payload = kycService.image(verificationId, side, reviewer)
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(payload.contentType))
			.cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"kyc-${verificationId}-${side.name.lowercase()}.img\"")
			.body(payload.bytes)
	}
}