package com.britam.insureclaim.audit

import com.britam.insureclaim.common.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "Audit trail", description = "Who did what, when — recorded from annotated endpoints")
@RestController
@RequestMapping("/api/v1/admin/audit")
@PreAuthorize("hasAnyRole('INSURER_ADMIN', 'ADMIN')")
class AuditController(private val auditService: AuditService) {

	@Operation(summary = "Search the audit trail (newest first)")
	@GetMapping
	fun list(
		@Parameter(description = "Match action, description, actor email, entity id or path")
		@RequestParam(required = false) query: String? = null,
		@Parameter(description = "Exact action code, e.g. CLAIM_CREATE")
		@RequestParam(required = false) action: String? = null,
		@Parameter(description = "true = only successes, false = only failures")
		@RequestParam(required = false) success: Boolean? = null,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate? = null,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate? = null,
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "20") size: Int = 20,
	): PageResponse<AuditLogResponse> =
		auditService.search(query, action, success, from, to, page, size)
}
