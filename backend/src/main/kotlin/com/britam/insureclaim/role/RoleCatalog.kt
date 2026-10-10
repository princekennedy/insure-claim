package com.britam.insureclaim.role

import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.common.ValidationException
import com.britam.insureclaim.user.UserRepository
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class PermissionDefinition(
	val code: String,
	val name: String,
	val description: String,
)

data class RoleDefinition(
	val code: String,
	val name: String,
	val description: String,
	@param:Schema(description = "True for the four code-defined system roles")
	val system: Boolean,
	val permissions: List<PermissionDefinition>,
)

@Schema(name = "RoleCreateRequest", description = "New role an administrator creates")
data class RoleCreateRequest(
	@field:NotBlank
	@field:Pattern(
		regexp = "^[A-Z][A-Z0-9_]{2,31}$",
		message = "must be 3-32 characters of A-Z, 0-9 or underscore, starting with a letter",
	)
	val code: String,
	@field:NotBlank @field:Size(min = 2, max = 80) val name: String,
	@field:Size(max = 255) val description: String = "",
	val permissionCodes: List<String> = emptyList(),
)

@Schema(name = "RoleUpdateRequest", description = "Editable fields of an existing role")
data class RoleUpdateRequest(
	@field:NotBlank @field:Size(min = 2, max = 80) val name: String,
	@field:Size(max = 255) val description: String = "",
	val permissionCodes: List<String> = emptyList(),
)

/**
 * Admin-managed role catalog backed by the `roles` / `permissions` /
 * `role_permissions` tables (V7). The four system roles are seeded on boot;
 * administrators can create, edit and retire extra roles from the UI.
 * Authorization itself still runs on role codes: every non-CUSTOMER role
 * receives ROLE_STAFF, so a new role immediately works across the staff
 * consoles without touching any security matcher.
 */
@Service
class RoleCatalog(
	private val roleRepository: RoleRepository,
	private val permissionRepository: PermissionRepository,
	private val rolePermissionRepository: RolePermissionRepository,
	private val userRepository: UserRepository,
) {

	@Transactional(readOnly = true)
	fun roles(page: Int, size: Int): PageResponse<RoleDefinition> {
		val pageable = PageRequest.of(
			page.coerceAtLeast(0),
			size.coerceIn(1, 100),
			Sort.by(Sort.Direction.ASC, "code"),
		)
		val rolePage = roleRepository.findAll(pageable)
		val grants = grantsFor(rolePage.content.map { it.code })
		val permissions = permissionIndex()
		return PageResponse.map(rolePage) { entity ->
			entity.toDefinition(grants[entity.code].orEmpty(), permissions)
		}
	}

	@Transactional(readOnly = true)
	fun role(code: String): RoleDefinition {
		val entity = findOrThrow(code)
		val grants = grantsFor(listOf(entity.code))
		return entity.toDefinition(grants[entity.code].orEmpty(), permissionIndex())
	}

	@Transactional(readOnly = true)
	fun permissions(): List<PermissionDefinition> =
		permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "code"))
			.map { PermissionDefinition(it.code, it.name, it.description) }

	@Transactional
	fun create(request: RoleCreateRequest): RoleDefinition {
		val code = request.code.trim().uppercase()
		if (code == STAFF_AUTHORITY) {
			throw ValidationException("STAFF is a reserved authority and cannot be used as a role code", "RESERVED_ROLE_CODE")
		}
		if (roleRepository.existsById(code)) {
			throw ConflictException("A role with code $code already exists", "ROLE_EXISTS")
		}
		val permissionCodes = validatePermissions(request.permissionCodes)
		val entity = roleRepository.save(
			RoleEntity(
				code = code,
				name = request.name.trim(),
				description = request.description.trim().take(255),
				system = false,
			),
		)
		replaceGrants(code, permissionCodes)
		val grants = grantsFor(listOf(code))
		return entity.toDefinition(grants[code].orEmpty(), permissionIndex())
	}

	@Transactional
	fun update(code: String, request: RoleUpdateRequest): RoleDefinition {
		val entity = findOrThrow(code)
		val permissionCodes = validatePermissions(request.permissionCodes)
		entity.name = request.name.trim()
		entity.description = request.description.trim().take(255)
		entity.updatedAt = Instant.now()
		roleRepository.save(entity)
		replaceGrants(entity.code, permissionCodes)
		val grants = grantsFor(listOf(entity.code))
		return entity.toDefinition(grants[entity.code].orEmpty(), permissionIndex())
	}

	@Transactional
	fun delete(code: String) {
		val entity = findOrThrow(code)
		if (entity.system) {
			throw BusinessRuleException("System roles are defined in code and cannot be deleted", "SYSTEM_ROLE")
		}
		if (userRepository.existsByRole(entity.code)) {
			throw ConflictException(
				"Users still have the ${entity.code} role - reassign them first",
				"ROLE_IN_USE",
			)
		}
		rolePermissionRepository.deleteByRoleCode(entity.code)
		roleRepository.delete(entity)
	}

	private fun findOrThrow(code: String): RoleEntity =
		roleRepository.findById(code.trim().uppercase())
			.orElseThrow { NotFoundException("Role", code) }

	private fun validatePermissions(codes: List<String>): List<String> {
		val requested = codes.map { it.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
		if (requested.isEmpty()) return requested
		val known = permissionRepository.findAllById(requested).map { it.code }.toSet()
		val unknown = requested - known
		if (unknown.isNotEmpty()) {
			throw ValidationException(
				"Unknown permission codes: ${unknown.joinToString(", ")}",
				"INVALID_PERMISSIONS",
			)
		}
		return requested.sorted()
	}

	private fun replaceGrants(roleCode: String, permissionCodes: List<String>) {
		rolePermissionRepository.deleteByRoleCode(roleCode)
		rolePermissionRepository.saveAll(
			permissionCodes.map { RolePermissionEntity(RolePermissionKey(roleCode, it)) },
		)
	}

	private fun grantsFor(roleCodes: List<String>): Map<String, List<String>> {
		if (roleCodes.isEmpty()) return emptyMap()
		return rolePermissionRepository.findAllByRoleCodes(roleCodes)
			.groupBy({ it.id.roleCode }, { it.id.permissionCode })
	}

	private fun permissionIndex(): Map<String, PermissionDefinition> =
		permissionRepository.findAll().associate {
			it.code to PermissionDefinition(it.code, it.name, it.description)
		}

	private fun RoleEntity.toDefinition(
		grants: List<String>,
		permissions: Map<String, PermissionDefinition>,
	): RoleDefinition = RoleDefinition(
		code = code,
		name = name,
		description = description,
		system = system,
		permissions = grants.mapNotNull { permissions[it] },
	)

	companion object {
		/** Mirrors the ROLE_STAFF authority handed to every non-CUSTOMER role. */
		const val STAFF_AUTHORITY = "STAFF"
	}
}
