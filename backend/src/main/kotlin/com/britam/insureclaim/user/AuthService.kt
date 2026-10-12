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
	private val roleRepository: com.britam.insureclaim.role.RoleRepository,
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

	fun forgotPassword(request: ForgotPasswordRequest) {
		val email = request.email.trim().lowercase(Locale.ROOT)
		val user = userRepository.findByEmailIgnoreCase(email).orElse(null) ?: return // Silently return
		user.resetToken = java.util.UUID.randomUUID().toString()
		user.resetTokenExpiresAt = Instant.now().plus(Duration.ofHours(1))
		userRepository.save(user)
		log.info("Generated password reset token for user {}: {} (simulate sending email)", user.id, user.resetToken)
	}

	fun resetPassword(request: ResetPasswordRequest) {
		val user = userRepository.findByResetToken(request.token)
			.orElseThrow { BusinessRuleException("Invalid or expired reset token", "INVALID_TOKEN") }

		val now = Instant.now()
		if (user.resetTokenExpiresAt == null || user.resetTokenExpiresAt!!.isBefore(now)) {
			throw BusinessRuleException("Invalid or expired reset token", "INVALID_TOKEN")
		}

		validatePasswordStrength(request.newPassword)
		user.password = passwordEncoder.encode(request.newPassword)!!
		user.resetToken = null
		user.resetTokenExpiresAt = null
		userRepository.save(user)
		refreshTokenRepository.findActiveByUser(user.id ?: 0L).forEach { it.revoke(now) }
		log.info("User {} reset their password successfully", user.id)
	}

	@Transactional(readOnly = true)
	fun findById(id: Long): User =
		userRepository.findById(id).orElseThrow { NotFoundException("User", id) }

	@Transactional(readOnly = true)
	fun search(
		query: String?,
		role: String?,
		pageable: Pageable,
	): Page<User> = userRepository.search(
		query = query?.trim()?.takeIf { it.isNotBlank() },
		role = role?.trim()?.takeIf { it.isNotBlank() }?.uppercase(),
		pageable = pageable,
	)

	fun updateRole(targetUserId: Long, role: String, actingUserId: Long) {
		val roleCode = requireKnownRole(role)
		val user = findById(targetUserId)
		if (user.id == actingUserId) {
			throw BusinessRuleException("You cannot change your own role", "SELF_ROLE_CHANGE")
		}
		if (user.role == roleCode) return
		user.role = roleCode
		userRepository.save(user)
		revokeSessions(user.id ?: targetUserId)
	}

	fun createUser(request: CreateUserRequest): User {
		val email = request.email.trim().lowercase(Locale.ROOT)
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw ConflictException("An account with this email already exists", "EMAIL_TAKEN")
		}
		validatePasswordStrength(request.password)
		val roleCode = requireKnownRole(request.role)
		val user = userRepository.save(
			User(
				email = email,
				password = passwordEncoder.encode(request.password)!!,
				fullName = request.fullName.trim(),
				phone = request.phone?.trim()?.takeIf { it.isNotBlank() },
				nic = request.nic?.trim()?.uppercase()?.takeIf { it.isNotBlank() },
				role = roleCode,
				enabled = true,
				emailVerified = true,
			),
		)
		log.info("Admin created user {} with role {}", email, roleCode)
		return user
	}

	fun updateUser(targetUserId: Long, request: UpdateUserRequest, actingUserId: Long): User {
		val user = findById(targetUserId)
		user.fullName = request.fullName.trim()
		user.phone = request.phone?.trim()?.takeIf { it.isNotBlank() }
		user.nic = request.nic?.trim()?.uppercase()?.takeIf { it.isNotBlank() }

		request.role?.let { requested ->
			val roleCode = requireKnownRole(requested)
			if (user.role != roleCode) {
				if (user.id == actingUserId) {
					throw BusinessRuleException("You cannot change your own role", "SELF_ROLE_CHANGE")
				}
				user.role = roleCode
				revokeSessions(user.id ?: targetUserId)
			}
		}

		request.enabled?.let { enabled ->
			if (user.id == actingUserId && !enabled) {
				throw BusinessRuleException("You cannot disable your own account", "SELF_DISABLE")
			}
			if (user.enabled != enabled) {
				user.enabled = enabled
				if (!enabled) revokeSessions(user.id ?: targetUserId)
			}
		}

		request.password?.let { raw ->
			validatePasswordStrength(raw)
			user.password = passwordEncoder.encode(raw)!!
			user.failedLoginCount = 0
			user.lockedUntil = null
			revokeSessions(user.id ?: targetUserId)
		}

		return userRepository.save(user)
	}

	fun deleteUser(targetUserId: Long, actingUserId: Long) {
		if (targetUserId == actingUserId) {
			throw BusinessRuleException("You cannot delete your own account", "SELF_DELETE")
		}
		val user = findById(targetUserId)
		if (user.role.equals(com.britam.insureclaim.role.Role.ADMIN.code, ignoreCase = true) &&
			userRepository.countByRole(com.britam.insureclaim.role.Role.ADMIN.code) <= 1L
		) {
			throw BusinessRuleException("The last administrator account cannot be deleted", "LAST_ADMIN")
		}
		userRepository.delete(user)
		try {
			userRepository.flush()
		} catch (ex: org.springframework.dao.DataIntegrityViolationException) {
			// claims.customer_id is ON DELETE RESTRICT - the only hard reference left.
			throw ConflictException(
				"That user still owns claims - disable the account instead",
				"USER_HAS_RECORDS",
			)
		}
		log.info("Admin deleted user {}", targetUserId)
	}

	private fun requireKnownRole(role: String): String {
		val code = role.trim().uppercase()
		if (!roleRepository.existsById(code)) {
			throw BusinessRuleException("Unknown role: $role", "INVALID_ROLE")
		}
		return code
	}

	private fun revokeSessions(userId: Long) {
		refreshTokenRepository.findActiveByUser(userId).forEach { it.revoke() }
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
		val role = user.role
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
