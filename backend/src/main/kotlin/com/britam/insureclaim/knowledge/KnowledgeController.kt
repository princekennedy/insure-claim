package com.britam.insureclaim.knowledge

import com.britam.insureclaim.common.Auditable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Knowledge base", description = "Company articles the AI assistant uses when answering customers")
@RestController
@RequestMapping("/api/v1/admin/knowledge")
class KnowledgeController(
	private val knowledgeService: KnowledgeService,
) {

	@Operation(summary = "Every knowledge article, newest first")
	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasAuthority('KNOWLEDGE_READ') or hasAuthority('KNOWLEDGE_MANAGE')")
	fun list(): List<KnowledgeEntryResponse> = knowledgeService.list()

	@Operation(summary = "Publish a new knowledge article")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN') or hasAuthority('KNOWLEDGE_MANAGE')")
	@Auditable(action = "KNOWLEDGE_CREATE", description = "Published a knowledge article", entityType = "KNOWLEDGE")
	fun create(@Valid @RequestBody request: KnowledgeEntryRequest): KnowledgeEntryResponse =
		knowledgeService.create(request)

	@Operation(summary = "Edit or enable/disable a knowledge article")
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN') or hasAuthority('KNOWLEDGE_MANAGE')")
	@Auditable(action = "KNOWLEDGE_UPDATE", description = "Updated a knowledge article", entityType = "KNOWLEDGE")
	fun update(
		@PathVariable id: Long,
		@Valid @RequestBody request: KnowledgeEntryRequest,
	): KnowledgeEntryResponse = knowledgeService.update(id, request)

	@Operation(summary = "Retire a knowledge article")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN') or hasAuthority('KNOWLEDGE_MANAGE')")
	@Auditable(action = "KNOWLEDGE_DELETE", description = "Deleted a knowledge article", entityType = "KNOWLEDGE")
	fun delete(@PathVariable id: Long) = knowledgeService.delete(id)
}
