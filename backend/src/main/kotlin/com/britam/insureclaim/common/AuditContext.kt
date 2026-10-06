package com.britam.insureclaim.common

import com.britam.insureclaim.security.AuthenticatedUser
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Answers "who is performing this write?" for the `created_by` / `updated_by`
 * columns that every entity carries.
 *
 * Two sources, in priority order:
 *  1. an explicit override, used by jobs and seeders that run outside a request
 *     (`AuditContext.actingAs(...)`); it is thread-local because the same
 *     thread may go on to serve a request later.
 *  2. the security principal placed there by the JWT filter.
 *
 * When neither yields a user the value stays null, which is the honest answer
 * for a system-driven insert - a fabricated id would be worse than a blank.
 */
object AuditContext {

	private val OVERRIDE = ThreadLocal<Long?>()

	fun actorId(): Long? = OVERRIDE.get()
		?: (SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser)?.id

	fun <T> actingAs(userId: Long?, block: () -> T): T {
		val previous = OVERRIDE.get()
		OVERRIDE.set(userId)
		try {
			return block()
		} finally {
			if (previous == null) OVERRIDE.remove() else OVERRIDE.set(previous)
		}
	}
}
