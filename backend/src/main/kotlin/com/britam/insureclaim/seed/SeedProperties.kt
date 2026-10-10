package com.britam.insureclaim.seed

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "insureclaim.seed")
data class SeedProperties(
	/**
	 * Master switch. Reference data (the garage panel) and the account/vehicle/
	 * policy records all come straight from the JSON files under classpath:seed,
	 * so a single flag decides whether anything is seeded at all.
	 */
	val enabled: Boolean = true,
)
