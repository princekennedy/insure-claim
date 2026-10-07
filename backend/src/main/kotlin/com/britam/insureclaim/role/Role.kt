package com.britam.insureclaim.role

/**
 * Role codes used across the portal. A user's role is stored as a plain
 * VARCHAR code on the users table, so [User.role] is a [String]; this class
 * is a value-object Constant holder for the canonical role definitions.
 */
class Role(val code: String, val name: String) {

    companion object {
        /** Staff role codes that can view all claims and perform administrative actions. */
        val STAFF_CODES: Set<String> = setOf(CUSTOMER.code, AGENT.code, INSURER_ADMIN.code)

        /** All valid role codes. */
        val VALID_CODES: Set<String> = setOf(CUSTOMER.code, AGENT.code, INSURER_ADMIN.code, ADMIN.code)

        /** Canonical role definitions. */
        val CUSTOMER = Role("CUSTOMER", "Policyholder")
        val AGENT = Role("AGENT", "Claims Agent")
        val INSURER_ADMIN = Role("INSURER_ADMIN", "Insurer Administrator")
        val ADMIN = Role("ADMIN", "Platform Administrator")

        /** Pseudo-enumeration used by seeders and admin UIs that expect enum-like access. */
        val entries: List<Role> = listOf(ADMIN, INSURER_ADMIN, AGENT, CUSTOMER)

        /** Looks up a role by its code, suitable for seed data and admin operations. */
        fun fromCode(code: String): Role =
            entries.firstOrNull { it.code == code.uppercase() }
                ?: throw IllegalArgumentException("Unknown role code: $code")
    }

    override fun equals(other: Any?): Boolean =
        other is Role && other.code == code

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = code
}

/** Extension function for authorization checks. */
fun Role.isStaff(): Boolean = code in STAFF_CODES

/** Resolves a role code to a human-readable display name. */
fun roleNameFor(code: String): String? = when (code.uppercase()) {
    "ADMIN" -> "Platform Administrator"
    "INSURER_ADMIN" -> "Insurer Administrator"
    "AGENT" -> "Claims Agent"
    "CUSTOMER" -> "Policyholder"
    else -> null
}
