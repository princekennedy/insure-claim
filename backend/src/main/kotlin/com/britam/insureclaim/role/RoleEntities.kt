package com.britam.insureclaim.role

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

/**
 * A row in the admin-managed role catalog. The four system roles are seeded
 * from `seed/roles.json` and flagged with [system] so the UI can protect them;
 * extra roles an administrator creates get authorization through ROLE_STAFF
 * (any non-CUSTOMER role is staff).
 */
@Entity
@Table(name = "roles")
class RoleEntity(
	@Id
	@Column(name = "code", nullable = false, length = 32)
	var code: String = "",

	@Column(name = "name", nullable = false, length = 80)
	var name: String = "",

	@Column(name = "description", nullable = false, length = 255)
	var description: String = "",

	@Column(name = "is_system", nullable = false)
	var system: Boolean = false,

	@Column(name = "created_at", nullable = false, updatable = false)
	var createdAt: Instant = Instant.now(),

	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "permissions")
class PermissionEntity(
	@Id
	@Column(name = "code", nullable = false, length = 64)
	var code: String = "",

	@Column(name = "name", nullable = false, length = 80)
	var name: String = "",

	@Column(name = "description", nullable = false, length = 255)
	var description: String = "",
)

@Embeddable
class RolePermissionKey(
	@Column(name = "role_code", nullable = false, length = 32)
	var roleCode: String = "",

	@Column(name = "permission_code", nullable = false, length = 64)
	var permissionCode: String = "",
)

@Entity
@Table(name = "role_permissions")
class RolePermissionEntity(
	@EmbeddedId
	var id: RolePermissionKey = RolePermissionKey(),
)

interface RoleRepository : JpaRepository<RoleEntity, String> {

	fun existsByCodeIgnoreCase(code: String): Boolean
}

interface PermissionRepository : JpaRepository<PermissionEntity, String>

interface RolePermissionRepository : JpaRepository<RolePermissionEntity, RolePermissionKey> {

	@Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.id.roleCode IN :roleCodes")
	fun findAllByRoleCodes(@Param("roleCodes") roleCodes: List<String>): List<RolePermissionEntity>

	@Modifying
	@Query("DELETE FROM RolePermissionEntity rp WHERE rp.id.roleCode = :roleCode")
	fun deleteByRoleCode(@Param("roleCode") roleCode: String): Int
}
