package com.britam.insureclaim.knowledge

import com.britam.insureclaim.common.NotFoundException
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Schema(name = "KnowledgeEntry", description = "One article the assistant can use when answering customers")
data class KnowledgeEntryResponse(
	val id: Long,
	val title: String,
	val category: String,
	val content: String,
	val enabled: Boolean,
	val createdAt: Instant,
	val updatedAt: Instant,
)

@Schema(name = "KnowledgeEntryRequest", description = "Article content; enabled articles are added to the assistant prompt")
data class KnowledgeEntryRequest(
	@field:NotBlank @field:Size(max = 160) val title: String,
	@field:Size(max = 64) val category: String = "General",
	@field:NotBlank @field:Size(max = 20000) val content: String,
	val enabled: Boolean = true,
)

/**
 * CRUD for the assistant knowledge base and the prompt text assembled from it.
 * Only enabled articles are sent to the model; the total is capped so a large
 * catalogue cannot crowd out the conversation or the verified claim context.
 */
@Service
class KnowledgeService(
	private val repository: KnowledgeEntryRepository,
) {

	@Transactional(readOnly = true)
	fun list(): List<KnowledgeEntryResponse> =
		repository.findAllByOrderByUpdatedAtDesc().map { it.toResponse() }

	@Transactional
	fun create(request: KnowledgeEntryRequest): KnowledgeEntryResponse =
		repository.save(
			KnowledgeEntry(
				title = request.title.trim(),
				category = request.category.trim().ifBlank { DEFAULT_CATEGORY },
				content = request.content.trim(),
				enabled = request.enabled,
			),
		).toResponse()

	@Transactional
	fun update(id: Long, request: KnowledgeEntryRequest): KnowledgeEntryResponse {
		val entry = findOrThrow(id)
		entry.title = request.title.trim()
		entry.category = request.category.trim().ifBlank { DEFAULT_CATEGORY }
		entry.content = request.content.trim()
		entry.enabled = request.enabled
		return repository.save(entry).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		repository.delete(findOrThrow(id))
	}

	/** Enabled articles formatted for the assistant's system prompt. */
	@Transactional(readOnly = true)
	fun promptText(): String {
		val builder = StringBuilder()
		for (entry in repository.findAllByEnabledTrueOrderByCategoryAscTitleAsc()) {
			val block = buildString {
				append("### ").append(entry.title.trim())
				if (entry.category.isNotBlank() && entry.category != DEFAULT_CATEGORY) {
					append(" (").append(entry.category.trim()).append(')')
				}
				append('\n').append(entry.content.trim())
			}
			if (builder.isNotEmpty()) builder.append("\n\n")
			val remaining = MAX_PROMPT_CHARS - builder.length
			if (remaining <= 0) break
			if (block.length > remaining) {
				builder.append(block.take(remaining))
				break
			}
			builder.append(block)
		}
		return builder.toString()
	}

	private fun findOrThrow(id: Long): KnowledgeEntry =
		repository.findById(id).orElseThrow { NotFoundException("Knowledge entry", id) }

	private fun KnowledgeEntry.toResponse(): KnowledgeEntryResponse = KnowledgeEntryResponse(
		id = id ?: 0L,
		title = title,
		category = category,
		content = content,
		enabled = enabled,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)

	private companion object {
		const val DEFAULT_CATEGORY = "General"
		const val MAX_PROMPT_CHARS = 30000
	}
}
