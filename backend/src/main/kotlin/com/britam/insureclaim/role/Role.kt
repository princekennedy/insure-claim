package com.britam.insureclaim.role

import com.britam.insureclaim.common.AuditableVersionedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "roles")
class Role(
    @Column(name = "code", nullable = false, unique = true, length = 32)
    var code: String = "",

    @Column(name = "name", nullable = false, length = 160)
    var name: String = "",

    @Column(name = "description", length = 255)
    var description: String? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")]
    )
    var permissions: MutableSet<com.britam.insureclaim.permission.Permission> = mutableSetOf(),
) : AuditableVersionedEntity() {

	companion object {
		/** Staff role codes that can view all claims and perform administrative actions. */
		val STAFF_CODES = setOf("ADMIN", "INSURER_ADMIN", "AGENT")

		/** Looks up a role by its code, suitable for seed data and admin operations. */
		fun fromCode(code: String): Role {
			return Role().apply { this.code = code.uppercase() }
		}

		/** Pseudo-enumeration used by seeders and admin UIs that expect enum-like access. */
		val entries: List<Role>
			get() = listOf(
				fromCode("ADMIN"),
				fromCode("INSURER_ADMIN"),
				fromCode("AGENT"),
				fromCode("CUSTOMER"),
			)

		/** Convenience constant for customer role — matches AuthService.Role.CUSTOMER usage. */
		val CUSTOMER: Role
			get() = fromCode("CUSTOMER")

		/** Convenience constants used by SecurityConfig: Role.ADMIN.name, Role.AGENT.name, Role.INSURER_ADMIN.name. */
		val ADMIN: Role
			get() = fromCode("ADMIN")
		val AGENT: Role
			get() = fromCode("AGENT")
		val INSURER_ADMIN: Role
			get() = fromCode("INSURER_ADMIN")
	}
}

/** Extension functions on Role for authorization checks. */
fun Role.isStaff(): Boolean = code in Role.STAFF_CODES