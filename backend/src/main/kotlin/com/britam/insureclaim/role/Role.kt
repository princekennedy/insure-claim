package com.britam.insureclaim.role

/**
 * Role codes used across the portal. Roles are stored as a plain VARCHAR
 * column on the users table rather than as a separate entity, so this class
 * is a value-object constant holder rather than a JPA entity.
 */
class Role

/** Extension functions on Role for authorization checks. */
fun Role.isStaff(): Boolean = this in Role.STAFF_CODES

/** Role code constants and lookup helpers. */
fun Role.Companion.fromCode(code: String): String = code.uppercase()

val Role.Companion.STAFF_CODES: Set<String>
	get() = setOf("ADMIN", "INSURER_ADMIN", "AGENT")

val Role.Companion.entries: List<String>
	get() = listOf(
		"ADMIN",
		"INSURER_ADMIN",
		"AGENT",
		"CUSTOMER",
	)

val Role.Companion.CUSTOMER: String
	get() = "CUSTOMER"

val Role.Companion.ADMIN: String
	get() = "ADMIN"

val Role.Companion.AGENT: String
	get() = "AGENT"

val Role.Companion.INSURER_ADMIN: String
	get() = "INSURER_ADMIN"

/** Resolves a role code to a human-readable display name. */
fun roleNameFor(code: String): String? = when (code.uppercase()) {
	"ADMIN" -> "Platform Administrator"
	"INSURER_ADMIN" -> "Insurer Administrator"
	"AGENT" -> "Claims Agent"
	"CUSTOMER" -> "Policyholder"
	else -> null
}

/** All valid role codes, for validation. */
val VALID_ROLE_CODES: Set<String> = setOf("ADMIN", "INSURER_ADMIN", "AGENT", "CUSTOMER")