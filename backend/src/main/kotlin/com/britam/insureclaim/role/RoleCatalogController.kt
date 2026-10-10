package com.britam.insureclaim.role

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Roles", description = "Role definitions and their permission grants")
@RestController
@RequestMapping("/api/v1/admin/roles")
@PreAuthorize("hasAnyRole('AGENT', 'INSURER_ADMIN', 'ADMIN')")
class RoleCatalogController(private val roleCatalog: RoleCatalog) {

	@Operation(summary = "Every role with its granted permissions")
	@GetMapping
	fun list(): List<RoleDefinition> = roleCatalog.roles()
}
