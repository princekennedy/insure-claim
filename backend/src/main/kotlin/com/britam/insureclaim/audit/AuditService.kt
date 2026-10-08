package com.britam.insureclaim.audit

import com.britam.insureclaim.common.PageResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class AuditService(private val auditLogRepository: AuditLogRepository) {

	private val log = LoggerFactory.getLogger(javaClass)

	/**
	 * Persist one audit row off the request thread. Runs async so a slow or
	 * broken write can never delay (or fail) the API response; the save itself
	 * is guarded so an audit problem degrades to a warning, never a 500.
	 */
	@Async
	fun record(entry: AuditLog) {
		try {
			auditLogRepository.save(entry)
		} catch (ex: Exception) {
			log.warn("Could not persist audit entry {} on {}", entry.action, entry.requestPath, ex)
		}
	}

	@Transactional(readOnly = true)
	fun search(
		query: String?,
		action: String?,
		success: Boolean?,
		from: LocalDate?,
		to: LocalDate?,
		page: Int,
		size: Int,
	): PageResponse<AuditLogResponse> {
		val pageable = PageRequest.of(
			page.coerceAtLeast(0),
			size.coerceIn(1, 100),
			Sort.by("createdAt").descending(),
		)
		val fromInstant = from?.atStartOfDay()?.toInstant(ZoneOffset.UTC)
		val toInstant = to?.plusDays(1)?.atStartOfDay()?.toInstant(ZoneOffset.UTC)
		return PageResponse.map(
			auditLogRepository.search(
				query = query?.trim()?.takeIf { it.isNotBlank() },
				action = action?.trim()?.takeIf { it.isNotBlank() },
				success = success,
				from = fromInstant,
				to = toInstant,
				pageable = pageable,
			),
		) { AuditLogResponse.from(it) }
	}
}
