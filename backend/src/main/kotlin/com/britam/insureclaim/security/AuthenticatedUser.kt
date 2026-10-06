package com.britam.insureclaim.security

import com.britam.insureclaim.user.Role
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Authenticated principal carried in the security context. Kept minimal — the
 * role is authoritative and the id is used for ownership checks.
 */
class AuthenticatedUser(
	val id: Long,
	val email: String,
	val role: Role,
	private val displayName: String,
) : UserDetails {

	override fun getAuthorities(): Collection<GrantedAuthority> =
		listOf(SimpleGrantedAuthority("ROLE_${role.name}"), SimpleGrantedAuthority("SCOPE_${role.name.lowercase()}"))

	override fun getPassword(): String? = null

	override fun getUsername(): String = email

	override fun isAccountNonExpired(): Boolean = true

	override fun isAccountNonLocked(): Boolean = true

	override fun isCredentialsNonExpired(): Boolean = true

	override fun isEnabled(): Boolean = true

	val isStaff: Boolean
		get() = role.isStaff()

	val name: String
		get() = displayName

	override fun toString(): String = "AuthenticatedUser(id=$id, role=$role)"
}
