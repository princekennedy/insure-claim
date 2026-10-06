package com.britam.insureclaim.user

import com.britam.insureclaim.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/**
 * Refresh tokens are stored hashed so a database leak cannot be replayed as a
 * live session. Rotation is enforced by [revokedAt]: every successful refresh
 * revokes the presented token and issues a new one, so a stolen token is only
 * usable until the legitimate client next refreshes.
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	var user: User = User(),

	@Column(name = "token_hash", nullable = false, unique = true, length = 128)
	var tokenHash: String = "",

	@Column(name = "issued_at", nullable = false)
	var issuedAt: Instant = Instant.now(),

	@Column(name = "expires_at", nullable = false)
	var expiresAt: Instant = Instant.now(),

	@Column(name = "revoked_at")
	var revokedAt: Instant? = null,

	@Column(name = "user_agent", length = 255)
	var userAgent: String? = null,

	@Column(name = "ip_address", length = 64)
	var ipAddress: String? = null,
) : BaseEntity() {

	fun isActive(now: Instant = Instant.now()): Boolean = revokedAt == null && expiresAt.isAfter(now)

	fun revoke(now: Instant = Instant.now()) {
		revokedAt = now
	}
}
