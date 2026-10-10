package com.britam.insureclaim.audit

import com.britam.insureclaim.common.PageResponse
import jakarta.persistence.criteria.Predicate
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
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
			Sort.by(Sort.Direction.DESC, "createdAt"),
		)

		val spec = Specification<AuditLog> { root, _, cb ->
			val predicates = mutableListOf<Predicate>()

			if (!query.isNullOrBlank()) {
				val pattern = "%${query.trim().lowercase()}%"
				predicates += cb.or(
					cb.like(cb.lower(root.get("action")), pattern),
					cb.like(cb.lower(root.get("description")), pattern),
					cb.like(cb.lower(cb.coalesce(root.get("actorEmail"), cb.literal(""))), pattern),
					cb.like(cb.lower(cb.coalesce(root.get("entityId"), cb.literal(""))), pattern),
					cb.like(cb.lower(root.get("requestPath")), pattern),
				)
			}
			if (!action.isNullOrBlank()) {
				predicates += cb.equal(root.get<String>("action"), action.trim())
			}
			if (success != null) {
				predicates += cb.equal(root.get<Boolean>("success"), success)
			}
			if (from != null) {
				predicates += cb.greaterThanOrEqualTo(
					root.get("createdAt"),
					from.atStartOfDay().atZone(ZoneOffset.UTC).toInstant(),
				)
			}
			if (to != null) {
				predicates += cb.lessThan(
					root.get("createdAt"),
					to.plusDays(1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant(),
				)
			}
			cb.and(*predicates.toTypedArray())
		}

		return PageResponse.map(auditLogRepository.findAll(spec, pageable)) { AuditLogResponse.from(it) }
	}
}
