package com.britam.insureclaim.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Authenticated principal carried in the security context.
 */
class AuthenticatedUser(
	val id: Long,
	val email: String,
	val role: String,
	private val displayName: String,
	val permissions: Collection<String> = emptyList(),
) : UserDetails {

	/**
	 * Every non-CUSTOMER role also receives ROLE_STAFF, so admin-created
	 * roles work across the staff consoles without editing security matchers.
	 * Role permissions come from the admin-managed role catalogue and let
	 * endpoints grant access without naming a specific role.
	 */
	override fun getAuthorities(): Collection<GrantedAuthority> = buildList {
		add(SimpleGrantedAuthority("ROLE_${role}"))
		if (role != CUSTOMER_ROLE) add(SimpleGrantedAuthority(STAFF_AUTHORITY))
		add(SimpleGrantedAuthority("SCOPE_${role.lowercase()}"))
		permissions.forEach { add(SimpleGrantedAuthority(it)) }
	}

	override fun getPassword(): String? = null

	override fun getUsername(): String = email

	override fun isAccountNonExpired(): Boolean = true

	override fun isAccountNonLocked(): Boolean = true

	override fun isCredentialsNonExpired(): Boolean = true

	override fun isEnabled(): Boolean = true

	val isStaff: Boolean
		get() = role != CUSTOMER_ROLE

	val name: String
		get() = displayName

	override fun toString(): String = "AuthenticatedUser(id=$id, role=$role)"

	companion object {
		const val CUSTOMER_ROLE = "CUSTOMER"
		const val STAFF_AUTHORITY = "ROLE_STAFF"
	}
}
