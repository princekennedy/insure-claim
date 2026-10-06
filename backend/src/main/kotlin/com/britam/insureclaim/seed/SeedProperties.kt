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
	 * Password applied to every account in `seed/users.json`. Deliberately has
	 * no default: nothing is written to the repository, and if it is left blank
	 * the account/vehicle/policy seeds are skipped rather than created with a
	 * guessable credential. Supply it through INSURECLAIM_SEED_PASSWORD.
	 */
	val password: String = "",
) {
	val canSeedAccounts: Boolean
		get() = enabled && password.isNotBlank()
}
