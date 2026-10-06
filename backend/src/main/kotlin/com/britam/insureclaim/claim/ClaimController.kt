package com.britam.insureclaim.claim

import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.security.CurrentUserResolver
import com.britam.insureclaim.user.UserAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@Tag(name = "Claims", description = "File claims, upload evidence, track progress and manage the claim queue")
@RestController
@RequestMapping("/api/v1/claims")
class ClaimController(
	private val claimService: ClaimService,
	private val accountService: UserAccountService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(
		summary = "File a new claim",
		description = "Validates that the vehicle has an in-force policy covering the incident date, then screens the claim for fraud.",
	)
	@PostMapping
	fun fileClaim(@Valid @RequestBody request: FileClaimRequest): ResponseEntity<ClaimDetailResponse> {
		val actor = accountService.requireCustomer(currentUser.requireId())
		val claim = claimService.fileClaim(actor, request)
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(claimService.toDetail(claim, actor))
	}

	@Operation(summary = "List claims (scoped to your own account, or all claims for staff)")
	@GetMapping
	fun list(
		@Parameter(description = "Filter by one or more claim statuses")
		@RequestParam(required = false) status: Set<ClaimStatus>? = null,
		@RequestParam(required = false) incidentType: Set<IncidentType>? = null,
		@Parameter(description = "Only claims flagged by fraud screening")
		@RequestParam(defaultValue = "false") fraudOnly: Boolean = false,
		@Parameter(description = "Match claim number, vehicle registration or description")
		@RequestParam(required = false) query: String? = null,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate? = null,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate? = null,
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "20") size: Int = 20,
	): PageResponse<ClaimSummaryResponse> {
		val actor = accountService.requireCustomer(currentUser.requireId())
		return claimService.search(
			actor = actor,
			statuses = status,
			incidentTypes = incidentType,
			fraudOnly = fraudOnly,
			query = query,
			from = from,
			to = to,
			page = page,
			size = size,
		)
	}

	@Operation(summary = "Full claim view with timeline, documents and next available stages")
	@GetMapping("/{claimId}")
	fun get(@PathVariable claimId: Long): ClaimDetailResponse {
		val actor = accountService.requireCustomer(currentUser.requireId())
		return claimService.toDetail(claimService.findAccessible(claimId, actor), actor)
	}

	@Operation(summary = "Full claim view by claim number")
	@GetMapping("/by-number/{claimNumber}")
	fun getByNumber(@PathVariable claimNumber: String): ClaimDetailResponse {
		val actor = accountService.requireCustomer(currentUser.requireId())
		return claimService.toDetail(claimService.findByNumber(claimNumber, actor), actor)
	}

	@Operation(summary = "Move a claim to a new status")
	@PatchMapping("/{claimId}/status")
	fun transition(
		@PathVariable claimId: Long,
		@Valid @RequestBody request: UpdateClaimStatusRequest,
	): ClaimDetailResponse {
		val actor = accountService.requireCustomer(currentUser.requireId())
		val claim = claimService.findAccessible(claimId, actor)
		val updated = claimService.transition(claim, request.status, request, actor)
		return claimService.toDetail(updated, actor)
	}

	@Operation(summary = "Upload a photo, police report or other evidence")
	@PostMapping("/{claimId}/documents", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	fun uploadDocument(
		@PathVariable claimId: Long,
		@RequestPart("file") file: MultipartFile,
		@RequestParam(defaultValue = "OTHER") documentType: ClaimDocumentType,
	): ResponseEntity<ClaimDocumentResponse> {
		val actor = accountService.requireCustomer(currentUser.requireId())
		val claim = claimService.findAccessible(claimId, actor)
		if (!actor.role.isStaff() && claim.status.isTerminal) {
			throw BusinessRuleException(
				"Evidence cannot be added to a ${claim.status.isCustomerVisibleLabel.lowercase()} claim",
				"CLAIM_CLOSED",
			)
		}
		val saved = claimService.addDocument(claim, file, documentType, actor)
		return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
	}

	@Operation(summary = "Remove an uploaded document")
	@DeleteMapping("/{claimId}/documents/{documentId}")
	fun deleteDocument(
		@PathVariable claimId: Long,
		@PathVariable documentId: Long,
	): ResponseEntity<Void> {
		val actor = accountService.requireCustomer(currentUser.requireId())
		val claim = claimService.findAccessible(claimId, actor)
		claimService.deleteDocument(claim, documentId, actor)
		return ResponseEntity.noContent().build()
	}

	@Operation(summary = "Issue or reuse a public tracking link for this claim")
	@PostMapping("/{claimId}/tracking-link")
	fun trackingLink(@PathVariable claimId: Long): TrackingLinkResponse {
		val actor = accountService.requireCustomer(currentUser.requireId())
		val claim = claimService.findAccessible(claimId, actor)
		val token = claimService.issueTrackingToken(claim)
		return TrackingLinkResponse(
			claimNumber = claim.claimNumber,
			token = token,
			path = "/track/$token",
		)
	}
}

data class TrackingLinkResponse(
	val claimNumber: String,
	val token: String,
	val path: String,
)
