package com.britam.insureclaim.user

import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.UnauthorizedException
import com.britam.insureclaim.role.Role	import com.britam.insureclaim.security.JwtService
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

	private fun getCustomerRole(): com.britam.insureclaim.role.Role =
		roleRepository.findByCode("CUSTOMER").orElseGet {
			roleRepository.findAll().firstOrNull() ?: throw IllegalStateException("No roles seeded")
		}

	fun register(request: RegisterRequest): TokenResponse {
		validatePasswordStrength(request.password)
		val email = request.email.trim().lowercase(Locale.ROOT)
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw ConflictException("An account with this email already exists", "EMAIL_TAKEN")
		}
		val user = User(
			email = email,
			passwordHash = passwordEncoder.encode(request.password)!!,
			fullName = request.fullName.trim(),
			phone = request.phone?.trim(),
			nic = request.nic?.trim()?.uppercase(),
			role = getCustomerRole(),
			enabled = true,
		)
		val saved = userRepository.save(user)
		log.info("Registered new customer {saved.id}")
		return issueTokenPair(saved)
	}
