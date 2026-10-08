package com.britam.insureclaim.policy

import com.britam.insureclaim.common.Auditable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Policies", description = "Insurer staff: issue and amend motor cover")
@PreAuthorize("hasAnyRole('AGENT','INSURER_ADMIN','ADMIN')")
@RestController
@RequestMapping("/api/v1/policies")
class PolicyController(
	private val policyService: PolicyService,
) {

	@Operation(summary = "Issue a policy for a customer's vehicle")
	@PostMapping
	@Auditable(action = "POLICY_CREATE", description = "Issued a policy", entityType = "POLICY", entityIdFromResponse = true)
	fun create(@Valid @RequestBody request: CreatePolicyRequest): ResponseEntity<PolicyAdminResponse> =
		ResponseEntity.status(HttpStatus.CREATED).body(policyService.create(request))

	@Operation(summary = "Amend an existing policy")
	@PutMapping("/{policyId}")
	@Auditable(action = "POLICY_UPDATE", description = "Amended a policy", entityType = "POLICY")
	fun update(
		@PathVariable policyId: Long,
		@Valid @RequestBody request: UpdatePolicyRequest,
	): PolicyAdminResponse = policyService.update(policyId, request)

	@Operation(summary = "Search policies issued by the portal")
	@GetMapping
	fun list(
		@RequestParam(required = false) customerId: Long?,
		@RequestParam(required = false) status: PolicyStatus?,
		@RequestParam(required = false) query: String?,
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "20") size: Int,
	) = policyService.list(customerId, status, query, page, size)
}
