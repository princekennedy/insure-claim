package com.britam.insureclaim.common

/**
 * Marks a controller endpoint whose invocation is recorded in the audit
 * trail. The aspect never captures method arguments or request bodies so
 * passwords, tokens and identity documents stay out of the log.
 *
 * @param action stable machine code, e.g. CLAIM_STATUS_UPDATE
 * @param description human sentence shown on the audit trail page
 * @param entityType entity kind the action touched, e.g. CLAIM
 * @param entityIdFromResponse read the created entity's id off the return value
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Auditable(
	val action: String,
	val description: String,
	val entityType: String = "",
	val entityIdFromResponse: Boolean = false,
)
