package com.britam.insureclaim.user

import com.britam.insureclaim.common.AuditableVersionedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "users")
class User(
	@Column(name = "email", nullable = false, unique = true, length = 255)
	var email: String = "",

	@Column(name = "password_hash", nullable = false, length = 255)
	var passwordHash: String = "",

	@Column(name = "full_name", nullable = false, length = 160)
	var fullName: String = "",

	@Column(name = "phone", length = 32)
	var phone: String? = null,

	@Column(name = "nic", length = 32)
	var nic: String? = null,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id")
	var role: com.britam.insureclaim.role.Role? = null,

	@Column(name = "enabled", nullable = false)
	var enabled: Boolean = true,

	@Column(name = "email_verified", nullable = false)
	var emailVerified: Boolean = false,

	@Column(name = "failed_login_count", nullable = false)
	var failedLoginCount: Int = 0,

	@Column(name = "locked_until")
	var lockedUntil: Instant? = null,

	@Column(name = "last_login_at")
	var lastLoginAt: Instant? = null,
) : AuditableVersionedEntity() {
	fun isLocked(now: Instant = Instant.now()): Boolean = lockedUntil?.isAfter(now) == true
	fun isUsable(): Boolean = enabled && !isLocked()
	fun fullNameOrEmail(): String = fullName.ifBlank { email }
	fun initials(): String = fullName.ifBlank { email.take(2).uppercase() }
}

data class UserSummary(
	val id: Long,
	val email: String,
	val fullName: String,
	val role: com.britam.insureclaim.role.Role,
)

fun User.toSummary(): UserSummary = UserSummary(
	id = id ?: 0L,
	email = email,
	fullName = fullName,
	role = role ?: com.britam.insureclaim.role.Role.CUSTOMER,
)

/** Authorization helpers that mirror the Role extension functions. */

fun User.isStaff(): Boolean = role?.code in com.britam.insureclaim.role.Role.STAFF_CODES
fun User.canViewAllClaims(): Boolean = role?.code in com.britam.insureclaim.role.Role.STAFF_CODES
