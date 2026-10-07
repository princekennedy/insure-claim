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
	var nic: String? = null,	@Column(name = "role", nullable = false, length = 32)
	var role: String = com.britam.insureclaim.role.CUSTOMER,

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
	fun isStaff(): Boolean = role in com.britam.insureclaim.role.STAFF_CODES
	fun canViewAllClaims(): Boolean = isStaff()
}

data class UserSummary(
	val id: Long,
	val email: String,
	val fullName: String,
	val role: String,
)

fun User.toSummary(): UserSummary = UserSummary(
	id = id ?: 0L,
	email = email,
	fullName = fullName,
	role = role,
)
