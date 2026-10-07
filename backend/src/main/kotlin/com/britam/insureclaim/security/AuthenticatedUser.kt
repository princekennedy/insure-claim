package com.britam.insureclaim.security

import com.britam.insureclaim.role.Role
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Authenticated principal carried in the security context.
 */
class AuthenticatedUser(
	val id: Long,
	val email: String,
	val role: Role,
	private val displayName: String,
) : UserDetails {

	override fun getAuthorities(): Collection<GrantedAuthority> =
		listOf(SimpleGrantedAuthority("ROLE_${role.code}"), SimpleGrantedAuthority("SCOPE_${role.code.lowercase()}"))

	override fun getPassword(): String? = null

	override fun getUsername(): String = email

	override fun isAccountNonExpired(): Boolean = true

	override fun isAccountNonLocked(): Boolean = true

	override fun isCredentialsNonExpired(): Boolean = true

	override fun isEnabled(): Boolean = true

	val isStaff: Boolean
		get() = role.code in setOf("ADMIN", "INSURER_ADMIN", "AGENT")

	val name: String
		get() = displayName

	override fun toString(): String = "AuthenticatedUser(id=$id, role=${role.code})"
}
