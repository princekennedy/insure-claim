package com.britam.insureclaim.security

import com.britam.insureclaim.common.UnauthorizedException
import com.britam.insureclaim.role.Role
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * Reads the principal out of the security context. Controllers and services use
 * this instead of touching SecurityContextHolder directly, so authorisation is
 * easy to audit in one place.
 */
@Component
class CurrentUserResolver {

	fun require(): AuthenticatedUser =
		SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser
			?: throw UnauthorizedException()

	fun currentOrNull(): AuthenticatedUser? =
		SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser

	fun requireId(): Long = require().id

	fun requireEmail(): String = require().email

	fun requireRole(): Role = require().role

	fun isStaff(): Boolean = require().isStaff
}
