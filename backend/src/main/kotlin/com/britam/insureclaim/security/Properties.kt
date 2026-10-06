package com.britam.insureclaim.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "insureclaim.jwt")
data class JwtProperties(
	/** HMAC-SHA256 signing key. Must be at least 32 bytes. */
	val secret: String,
	val issuer: String = "insureclaim-portal",
	val accessTokenTtl: Duration = Duration.ofMinutes(15),
	val refreshTokenTtl: Duration = Duration.ofDays(7),
) {
	init {
		require(secret.toByteArray().size >= 32) {
			"JWT secret must be at least 32 bytes for HS256. Set JWT_SECRET."
		}
	}
}

@ConfigurationProperties(prefix = "insureclaim.cors")
data class CorsProperties(
	val allowedOrigins: List<String> = listOf("http://localhost:3000"),
)

@ConfigurationProperties(prefix = "insureclaim.claims")
data class ClaimProperties(
	val maxOpenClaimsPerCustomer: Int = 5,
	val settlementWindowDays: Int = 30,
)

@ConfigurationProperties(prefix = "insureclaim.fraud")
data class FraudProperties(
	val autoAlertThreshold: Int = 60,
	val highRiskThreshold: Int = 80,
)

@ConfigurationProperties(prefix = "insureclaim.garage")
data class GarageProperties(
	val performanceWindowDays: Int = 90,
	val underperformingRatingThreshold: java.math.BigDecimal = java.math.BigDecimal("3.0"),
	val underperformingComplaintThreshold: Int = 5,
	/** Ratings below this put a garage on the watchlist. */
	val watchRatingThreshold: java.math.BigDecimal = java.math.BigDecimal("3.5"),
	/** Ratings below this count as underperforming. */
	val poorRatingThreshold: java.math.BigDecimal = java.math.BigDecimal("2.5"),
	/** Complaints at or above this count as underperforming, at or above the suspension value it is pulled off the panel. */
	val watchComplaintThreshold: Int = 2,
	val suspensionComplaintThreshold: Int = 8,
	/** Average turnaround above this is treated as slow service. */
	val slowTurnaroundDays: java.math.BigDecimal = java.math.BigDecimal("14"),
	/** Ratings are only trusted once this many exist. */
	val minimumRatingCount: Int = 3,
)

@ConfigurationProperties(prefix = "insureclaim.chatbot")
data class ChatbotProperties(
	val enabled: Boolean = true,
	val apiKey: String = "",
	val baseUrl: String = "https://api.anthropic.com",
	val model: String = "claude-sonnet-5",
	val maxTokens: Int = 700,
	val escalateScoreThreshold: Double = 0.65,
) {
	/** A real LLM is only used when a key is present; otherwise a rule-based bot answers. */
	val hasLlmProvider: Boolean
		get() = apiKey.isNotBlank()
}

@ConfigurationProperties(prefix = "insureclaim.storage")
data class StorageProperties(
	val uploadDir: String = "./data/uploads",
	val maxFileSizeBytes: Long = 10L * 1024 * 1024,
	val allowedContentTypes: List<String> = listOf("image/jpeg", "image/png", "image/pdf"),
)
