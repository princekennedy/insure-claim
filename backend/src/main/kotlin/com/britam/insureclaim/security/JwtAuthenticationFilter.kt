package com.britam.insureclaim.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Turns a valid `Authorization: Bearer <jwt>` header into an authenticated
 * principal. A missing or bad token is not an error here — the request simply
 * stays anonymous and the authorization rules decide the outcome.
 */
@Component
class JwtAuthenticationFilter(
	private val jwtService: JwtService,
	private val userRepository: com.britam.insureclaim.user.UserRepository,
) : OncePerRequestFilter() {

	private val log = LoggerFactory.getLogger(javaClass)

	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val token = resolveToken(request)
		if (token != null && SecurityContextHolder.getContext().authentication == null) {
			val parsed = jwtService.parse(token)
			if (parsed != null) {
				// Re-check the account so a disabled or locked user cannot keep
				// using an unexpired token.
				val account = userRepository.findById(parsed.userId).orElse(null)
				if (account == null) {
					log.debug("Token references unknown user {}", parsed.userId)
				} else if (!account.isUsable()) {
					log.debug("Rejecting token for unusable account {}", account.id)
				} else if (account.role != parsed.role) {
					log.debug("Rejecting token with stale role for account {}", account.id)
				} else {
					val principal = AuthenticatedUser(
						id = account.id ?: 0L,
						email = account.email,
						role = account.role,
						displayName = account.fullNameOrEmail(),
					)
					val authentication = UsernamePasswordAuthenticationToken(
						principal,
						null,
						principal.authorities,
					).apply {
						details = WebAuthenticationDetailsSource().buildDetails(request)
					}
					SecurityContextHolder.getContext().authentication = authentication
				}
			}
		}
		filterChain.doFilter(request, response)
	}

	private fun resolveToken(request: HttpServletRequest): String? {
		val header = request.getHeader("Authorization")
		if (header != null && header.startsWith("Bearer ", ignoreCase = true)) {
			return header.substring(7).trim().takeIf { it.isNotEmpty() }
		}
		// Supports browser clients that cannot set headers (e.g. img/iframe fetches).
		request.cookies?.firstOrNull { it.name == ACCESS_COOKIE }?.value?.let { cookieToken ->
			if (cookieToken.isNotBlank()) return cookieToken
		}
		return null
	}

	companion object {
		const val ACCESS_COOKIE = "ic_access"
	}
}
