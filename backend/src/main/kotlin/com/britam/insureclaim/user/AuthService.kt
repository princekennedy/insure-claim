package com.britam.insureclaim.user

import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.UnauthorizedException
import com.britam.insureclaim.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Locale

@Service
@Transactional
class AuthService(
	private val userRepository: UserRepository,
	private val refreshTokenRepository: RefreshTokenRepository,
	private val passwordEncoder: PasswordEncoder,
	private val jwtService: JwtService,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val maxFailedAttempts = 5
	private val lockoutDuration: Duration = Duration.ofMinutes(15)

	private fun getCustomerRole(): String = "CUSTOMER"

	fun register(request: RegisterRequest): TokenResponse {
		validatePasswordStrength(request.password)
		val email = request.email.trim().lowercase(Locale.ROOT)
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw ConflictException("An account with this email already exists", "EMAIL_TAKEN")
		}
		val user = User(
			email = email,
			password = passwordEncoder.encode(request.password)!!,
			fullName = request.fullName.trim(),
			phone = request.phone?.trim(),
			nic = request.nic?.trim()?.uppercase(),
			role = getCustomerRole(),
			enabled = true,
		)
		val saved = userRepository.save(user)
		log.info("Registered new customer {}", saved.id)
		return issueTokenPair(saved)
	}

	fun login(request: LoginRequest, userAgent: String?, ipAddress: String?): TokenResponse {
		val email = request.email.trim().lowercase(Locale.ROOT)
		val user = userRepository.findByEmailIgnoreCase(email)
			.orElseThrow { UnauthorizedException("Invalid email or password", "INVALID_CREDENTIALS") }

		val now = Instant.now()
		if (user.isLocked(now)) {
			throw UnauthorizedException(
				"Account is temporarily locked. Try again after ${lockoutDuration.toMinutes()} minutes.",
				"ACCOUNT_LOCKED",
			)
		}
		if (!user.enabled) {
			throw UnauthorizedException("This account has been disabled", "ACCOUNT_DISABLED")
		}
		if (!passwordEncoder.matches(request.password, user.password)) {
			recordFailedLogin(user, now)
			throw UnauthorizedException("Invalid email or password", "INVALID_CREDENTIALS")
		}

		user.failedLoginCount = 0
		user.lockedUntil = null
		user.lastLoginAt = now
		userRepository.save(user)
		return issueTokenPair(user, userAgent, ipAddress)
	}

	fun refresh(refreshToken: String, userAgent: String?, ipAddress: String?): TokenResponse {
		val hash = hashToken(refreshToken)
		val stored = refreshTokenRepository.findByTokenHash(hash)
			.orElseThrow { UnauthorizedException("Invalid session. Please sign in again.", "INVALID_REFRESH_TOKEN") }

		val now = Instant.now()
		if (!stored.isActive(now)) {
			refreshTokenRepository.findActiveByUser(stored.user.id ?: 0L).forEach { it.revoke(now) }
			log.warn("Refresh token reuse detected for user {}; all sessions revoked", stored.user.id)
			throw UnauthorizedException("Session is no longer valid. Please sign in again.", "REFRESH_TOKEN_REVOKED")
		}

		val user = stored.user
		if (!user.isUsable()) {
			throw UnauthorizedException("Account is not active", "ACCOUNT_DISABLED")
		}

		stored.revoke(now)
		return issueTokenPair(user, userAgent, ipAddress)
	}

	fun logout(userId: Long, refreshToken: String?) {
		val now = Instant.now()
		if (refreshToken != null) {
			refreshTokenRepository.findByTokenHash(hashToken(refreshToken))
				.filter { it.user.id == userId }
				.ifPresent { it.revoke(now) }
		}
		refreshTokenRepository.findActiveByUser(userId).forEach { it.revoke(now) }
	}

	fun logoutAll(userId: Long) {
		refreshTokenRepository.findActiveByUser(userId).forEach { it.revoke() }
	}

	fun changePassword(userId: Long, request: ChangePasswordRequest) {
		val user = findById(userId)
		if (!passwordEncoder.matches(request.currentPassword, user.password)) {
			throw BusinessRuleException("Current password is incorrect", "WRONG_PASSWORD")
		}
		if (passwordEncoder.matches(request.newPassword, user.password)) {
			throw BusinessRuleException("New password must be different from the current one", "PASSWORD_UNCHANGED")
		}
		validatePasswordStrength(request.newPassword)
		user.password = passwordEncoder.encode(request.newPassword)!!
		userRepository.save(user)
		refreshTokenRepository.findActiveByUser(userId).forEach { it.revoke() }
	}

	@Transactional(readOnly = true)
	fun findById(id: Long): User =
		userRepository.findById(id).orElseThrow { NotFoundException("User", id) }

	@Transactional(readOnly = true)
	fun search(
		query: String?,
		role: com.britam.insureclaim.role.Role?,
		pageable: Pageable,
	): Page<User> = userRepository.search(
		query = query?.trim()?.takeIf { it.isNotBlank() },
		role = role?.code,
		pageable = pageable,
	)

	fun updateRole(targetUserId: Long, role: String, actingUserId: Long) {
		if (role !in com.britam.insureclaim.role.Role.VALID_CODES) {
			throw BusinessRuleException("Unknown role: $role", "INVALID_ROLE")
		}
		val user = findById(targetUserId)
		if (user.id == actingUserId) {
			throw BusinessRuleException("You cannot change your own role", "SELF_ROLE_CHANGE")
		}
		user.role = role
		userRepository.save(user)
		refreshTokenRepository.findActiveByUser(user.id ?: 0L).forEach { it.revoke() }
	}

	fun setEnabled(targetUserId: Long, enabled: Boolean, actingUserId: Long) {
		val user = findById(targetUserId)
		if (user.id == actingUserId) {
			throw BusinessRuleException("You cannot disable your own account", "SELF_DISABLE")
		}
		user.enabled = enabled
		if (!enabled) {
			refreshTokenRepository.findActiveByUser(targetUserId).forEach { it.revoke() }
		}
		userRepository.save(user)
	}

	fun pruneExpiredRefreshTokens(): Long =
		refreshTokenRepository.deleteByExpiresAtBefore(Instant.now().minus(Duration.ofDays(1)))

	private fun recordFailedLogin(user: User, now: Instant) {
		user.failedLoginCount += 1
		if (user.failedLoginCount >= maxFailedAttempts) {
			user.lockedUntil = now.plus(lockoutDuration)
			user.failedLoginCount = 0
			log.warn("Locked account {} after repeated failed logins", user.id)
		}
		userRepository.save(user)
	}

	private fun issueTokenPair(
		user: User,
		userAgent: String? = null,
		ipAddress: String? = null,
	): TokenResponse {
		val userId = user.id ?: throw IllegalStateException("User must be persisted before issuing tokens")
		val role = user.role ?: throw IllegalStateException("User ${user.id} has no assigned role")
		val access = jwtService.issueAccessToken(userId, user.email, role)
		val refreshValue = jwtService.newRefreshTokenValue()

		refreshTokenRepository.save(
			RefreshToken(
				user = user,
				tokenHash = hashToken(refreshValue),
				issuedAt = access.issuedAt,
				expiresAt = Instant.now().plus(jwtService.refreshTokenTtl()),
				userAgent = userAgent?.take(240),
				ipAddress = ipAddress,
			),
		)

		return TokenResponse(
			accessToken = access.token,
			refreshToken = refreshValue,
			expiresIn = access.expiresInSeconds,
			user = UserResponse.from(user),
		)
	}

	private fun validatePasswordStrength(password: String) {
		val problems = buildList {
			if (password.length < 10) add("at least 10 characters")
			if (password.none { it.isUpperCase() }) add("an upper-case letter")
			if (password.none { it.isLowerCase() }) add("a lower-case letter")
			if (password.none { it.isDigit() }) add("a digit")
			if (password.none { !it.isLetterOrDigit() }) add("a symbol")
		}
		if (problems.isNotEmpty()) {
			throw BusinessRuleException(
				"Password must contain ${problems.joinToString(", ")}",
				"WEAK_PASSWORD",
			)
		}
	}

	private fun hashToken(token: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
	}
}
