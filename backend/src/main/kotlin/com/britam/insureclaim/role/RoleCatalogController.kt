package com.britam.insureclaim.role

import com.britam.insureclaim.common.Auditable
import com.britam.insureclaim.common.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Roles", description = "Role definitions and their permission grants")
@RestController
@RequestMapping("/api/v1/admin/roles")
class RoleCatalogController(private val roleCatalog: RoleCatalog) {

	@Operation(summary = "One page of roles with their granted permissions")
	@GetMapping
	@PreAuthorize("hasAnyRole('AGENT', 'INSURER_ADMIN', 'ADMIN', 'STAFF')")
	fun list(
		@RequestParam(defaultValue = "0") page: Int = 0,
		@RequestParam(defaultValue = "50") size: Int = 50,
	): PageResponse<RoleDefinition> = roleCatalog.roles(page, size)

	@Operation(summary = "Every permission that can be granted to a role")
	@GetMapping("/permissions")
	@PreAuthorize("hasAnyRole('AGENT', 'INSURER_ADMIN', 'ADMIN', 'STAFF')")
	fun permissions(): List<PermissionDefinition> = roleCatalog.permissions()

	@Operation(summary = "Fetch a single role")
	@GetMapping("/{code}")
	@PreAuthorize("hasAnyRole('AGENT', 'INSURER_ADMIN', 'ADMIN', 'STAFF')")
	fun get(@PathVariable code: String): RoleDefinition = roleCatalog.role(code)

	@Operation(summary = "Create a role (platform admin only)")
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Auditable(action = "ROLE_CREATE", description = "Created a role", entityType = "ROLE")
	fun create(@Valid @RequestBody request: RoleCreateRequest): ResponseEntity<RoleDefinition> =
		ResponseEntity.status(HttpStatus.CREATED).body(roleCatalog.create(request))

	@Operation(summary = "Update a role's name, description and grants (platform admin only)")
	@PutMapping("/{code}")
	@PreAuthorize("hasRole('ADMIN')")
	@Auditable(action = "ROLE_UPDATE", description = "Updated a role", entityType = "ROLE")
	fun update(
		@PathVariable code: String,
		@Valid @RequestBody request: RoleUpdateRequest,
	): RoleDefinition = roleCatalog.update(code, request)

	@Operation(summary = "Delete a non-system role (platform admin only)")
	@DeleteMapping("/{code}")
	@PreAuthorize("hasRole('ADMIN')")
	@Auditable(action = "ROLE_DELETE", description = "Deleted a role", entityType = "ROLE")
	fun delete(@PathVariable code: String): ResponseEntity<Void> {
		roleCatalog.delete(code)
		return ResponseEntity.noContent().build()
	}
}
