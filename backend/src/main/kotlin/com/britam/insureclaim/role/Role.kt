package com.britam.insureclaim.role

/**
 * Role codes used across the portal.
 */
class Role(val code: String, val name: String) {

    companion object {
        /** Staff role codes that can view all claims and perform administrative actions. */
        val STAFF_CODES: Set<String> = setOf("AGENT", "INSURER_ADMIN", "ADMIN")

        /** All valid role codes. */
        val VALID_CODES: Set<String> = setOf("CUSTOMER", "AGENT", "INSURER_ADMIN", "ADMIN")

        /** Canonical role definitions. */
        val CUSTOMER = Role("CUSTOMER", "Policyholder")
        val AGENT = Role("AGENT", "Claims Agent")
        val INSURER_ADMIN = Role("INSURER_ADMIN", "Insurer Administrator")
        val ADMIN = Role("ADMIN", "Platform Administrator")

        /** Pseudo-enumeration used by seeders and admin UIs that expect enum-like access. */
        val entries: List<Role> = listOf(ADMIN, INSURER_ADMIN, AGENT, CUSTOMER)

        /** Looks up a role by its code. */
        fun fromCode(code: String): Role =
            entries.firstOrNull { it.code == code.uppercase() }
                ?: throw IllegalArgumentException("Unknown role code: $code")
    }

    override fun equals(other: Any?): Boolean =
        other is Role && other.code == code

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = code
}

fun Role.isStaff(): Boolean = code in Role.STAFF_CODES

fun roleNameFor(code: String): String? = when (code.uppercase()) {
    "ADMIN" -> "Platform Administrator"
    "INSURER_ADMIN" -> "Insurer Administrator"
    "AGENT" -> "Claims Agent"
    "CUSTOMER" -> "Policyholder"
    else -> null
}
