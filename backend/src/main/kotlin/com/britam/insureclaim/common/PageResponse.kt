package com.britam.insureclaim.common

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

/**
 * Stable pagination envelope. Spring's own `PageImpl` serialises with an
 * unstable shape, so responses use this instead.
 */
@Schema(name = "PageResponse", description = "A single page of results")
data class PageResponse<T>(
	val content: List<T>,
	val page: Int,
	val size: Int,
	val totalElements: Long,
	val totalPages: Int,
	val first: Boolean,
	val last: Boolean,
	val empty: Boolean,
) {
	companion object {
		fun <T : Any> from(page: Page<T>): PageResponse<T> = PageResponse(
			content = page.content,
			page = page.number,
			size = page.size,
			totalElements = page.totalElements,
			totalPages = page.totalPages,
			first = page.isFirst,
			last = page.isLast,
			empty = page.isEmpty,
		)

		/** Re-types a page while preserving its pagination metadata. */
		fun <A : Any, B : Any> map(page: Page<A>, mapper: (A) -> B): PageResponse<B> = PageResponse(
			content = page.content.map(mapper),
			page = page.number,
			size = page.size,
			totalElements = page.totalElements,
			totalPages = page.totalPages,
			first = page.isFirst,
			last = page.isLast,
			empty = page.isEmpty,
		)
	}
}
