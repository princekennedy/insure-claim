package com.britam.insureclaim.seed

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "insureclaim.seed")
data class SeedProperties(
	/**
	 * Master switch. Reference data (the garage panel) is always safe to seed;
	 * accounts are not, so production deployments must turn this off.
	 */
	val enabled: Boolean = true,

	/**
	 * Optional fallback password for account rows in `seed/users.json` that do
	 * not carry their own `password`. Normally each JSON entry supplies its own
	 * credential; this only covers rows without one (can be set through
	 * INSURECLAIM_SEED_PASSWORD).
	 */
	val password: String = "",
)
