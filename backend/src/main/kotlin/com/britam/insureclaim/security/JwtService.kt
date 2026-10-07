package com.britam.insureclaim.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

/**
 * Issues and validates the stateless access token.
 */
@Service
class JwtService(private val properties: JwtProperties) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val signer: MACSigner by lazy { MACSigner(secretBytes()) }
	private val verifier: MACVerifier by lazy { MACVerifier(secretBytes()) }

	private fun secretBytes(): ByteArray =
		properties.secret.toByteArray(Charsets.UTF_8)

	data class IssuedToken(
		val token: String,
		val issuedAt: Instant,
		val expiresAt: Instant,
	) {
		val expiresInSeconds: Long
			get() = Duration.between(Instant.now(), expiresAt).seconds.coerceAtLeast(0)
	}

	fun issueAccessToken(userId: Long, email: String, role: com.britam.insureclaim.role.Role): IssuedToken {
		val now = Instant.now()
		val expiry = now.plus(properties.accessTokenTtl)
		val claims = JWTClaimsSet.Builder()
			.subject(userId.toString())
			.issuer(properties.issuer)
			.audience("insureclaim-api")
			.jwtID(UUID.randomUUID().toString())
			.issueTime(Date.from(now))
			.expirationTime(Date.from(expiry))
			.claim("email", email)
			.claim("role", role.code)
			.claim("typ", "access")
			.build()

		val jwt = SignedJWT(JWSHeader.Builder(JWSAlgorithm.HS256).build(), claims)
		jwt.sign(signer)
		log.debug("Issued access token for user ${userId} expiring at ${expiry}")
		return IssuedToken(token = jwt.serialize(), issuedAt = now, expiresAt = expiry)
	}

	fun newRefreshTokenValue(): String {
		val bytes = ByteArray(48)
		java.security.SecureRandom().nextBytes(bytes)
		return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
	}

	data class ParsedToken(
		val userId: Long,
		val email: String,
		val role: String,
		val expiresAt: Instant,
	)

	fun parse(token: String): ParsedToken? = try {
		val jwt = SignedJWT.parse(token)
		if (!jwt.verify(verifier)) {
			log.debug("Rejected token: bad signature")
			return null
		}
		val claims = jwt.jwtClaimsSet
		if (claims.issuer != properties.issuer) {
			log.debug("Rejected token: unexpected issuer {claims.issuer}")
			return null
		}
		if (claims.getStringClaim("typ") != "access") {
			log.debug("Rejected token: not an access token")
			return null
		}
		val subject = claims.subject?.toLongOrNull()
		if (subject == null) {
			log.debug("Rejected token: subject is not numeric")
			return null
		}
		val email = claims.getStringClaim("email") ?: return null
		val roleCode = claims.getStringClaim("role") ?: return null
		val expiry = claims.expirationTime?.toInstant() ?: return null
		if (expiry.isBefore(Instant.now())) {
			log.debug("Rejected token: expired at ${expiry}")
			return null
		}
		ParsedToken(userId = subject, email = email, role = roleCode, expiresAt = expiry)
	} catch (ex: Exception) {
		log.debug("Failed to parse token: ${ex.message}")
		null
	}

	fun refreshTokenTtl(): Duration = properties.refreshTokenTtl

	fun accessTokenTtl(): Duration = properties.accessTokenTtl
}
