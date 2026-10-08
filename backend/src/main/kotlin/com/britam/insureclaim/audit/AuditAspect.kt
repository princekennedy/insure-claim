package com.britam.insureclaim.audit

import com.britam.insureclaim.common.ApiException
import com.britam.insureclaim.common.Auditable
import com.britam.insureclaim.security.AuthenticatedUser
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Records one audit row per @Auditable endpoint invocation. Arguments and
 * request bodies are deliberately ignored: only who, what, where and the
 * outcome are captured, so secrets can never leak into the trail. Failures
 * (ApiException or anything unexpected) are recorded too — a rejected action
 * is exactly the kind of activity an audit trail exists for.
 */
@Aspect
@Component
class AuditAspect(private val auditService: AuditService) {

	private val log = LoggerFactory.getLogger(javaClass)

	@Around("@annotation(auditable)")
	fun recordActivity(proceedingJoinPoint: ProceedingJoinPoint, auditable: Auditable): Any? {
		val request = currentRequest()
		val arguments = proceedingJoinPoint.args
		var result: Any? = null
		var status = 200
		var success = true
		var detail: String? = null
		try {
			result = proceedingJoinPoint.proceed()
			status = statusOf(result, request)
			return result
		} catch (ex: ApiException) {
			status = ex.status.value()
			success = false
			detail = ex.message
			throw ex
		} catch (ex: Throwable) {
			status = 500
			success = false
			detail = ex.message
			throw ex
		} finally {
			try {
				auditService.record(buildEntry(auditable, arguments, request, result, status, success, detail))
			} catch (ex: Exception) {
				log.warn("Audit hook failed for {}", auditable.action, ex)
			}
		}
	}

	private fun buildEntry(
		auditable: Auditable,
		arguments: Array<out Any?>,
		request: HttpServletRequest?,
		result: Any?,
		status: Int,
		success: Boolean,
		detail: String?,
	): AuditLog {
		val actor = currentActor()
		return AuditLog(
			actorId = actor?.id,
			actorEmail = actor?.email ?: emailFromArguments(arguments),
			actorRole = actor?.role,
			action = auditable.action,
			description = auditable.description,
			entityType = auditable.entityType.ifBlank { null },
			entityId = idFromResult(result) ?: idFromPath(request),
			requestMethod = request?.method ?: "GET",
			requestPath = pathOf(request),
			status = status,
			success = success,
			detail = detail?.take(512),
			ipAddress = clientIp(request),
		)
	}

	private fun currentRequest(): HttpServletRequest? =
		(RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

	private fun currentActor(): AuthenticatedUser? =
		SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser

	/**
	 * Pre-authentication endpoints (login, register, forgot-password) have no
	 * principal yet; their first argument usually carries the email. Reading
	 * just getEmail() keeps passwords and tokens out of the record.
	 */
	private fun emailFromArguments(arguments: Array<out Any?>): String? =
		arguments.firstNotNullOfOrNull { argument ->
			if (argument == null) return@firstNotNullOfOrNull null
			runCatching {
				val getter = argument.javaClass.methods
					.firstOrNull { it.name == "getEmail" && it.parameterCount == 0 }
				(getter?.invoke(argument) as? String)?.takeIf { it.isNotBlank() }
			}.getOrNull()
		}

	private fun statusOf(result: Any?, request: HttpServletRequest?): Int =
		(result as? ResponseEntity<*>)?.statusCode?.value()
			?: request?.response?.status?.takeIf { it in 100..599 }
			?: 200

	private fun idFromResult(result: Any?): String? {
		val target = when {
			result == null -> null
			result is ResponseEntity<*> -> result.body
			else -> result
		} ?: return null
		return runCatching {
			val getter = target.javaClass.methods
				.firstOrNull { it.name == "getId" && it.parameterCount == 0 }
			getter?.invoke(target)?.toString()?.takeIf { it.isNotBlank() && it != "0" }
		}.getOrNull()
	}

	private fun idFromPath(request: HttpServletRequest?): String? {
		val path = request?.requestURI ?: return null
		return path.split('/')
			.lastOrNull { segment -> segment.isNotEmpty() && segment.all { it.isDigit() } }
	}

	private fun pathOf(request: HttpServletRequest?): String {
		if (request == null) return ""
		val query = request.queryString
		return (if (query.isNullOrBlank()) request.requestURI else "${request.requestURI}?$query").take(255)
	}

	private fun clientIp(request: HttpServletRequest?): String? =
		request?.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
			?: request?.remoteAddr
}
