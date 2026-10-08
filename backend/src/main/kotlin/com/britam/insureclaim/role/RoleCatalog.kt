package com.britam.insureclaim.role

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

data class PermissionDefinition(
	val code: String,
	val name: String,
	val description: String,
)

data class RoleDefinition(
	val code: String,
	val name: String,
	val description: String,
	val permissions: List<PermissionDefinition>,
)

/**
 * Serves the role catalog to the portal. Roles and permissions are
 * code-defined (validated at boot by JsonDataSeeder, never persisted), so
 * this reads the same seed JSON the seeder validates — one source of truth.
 */
@Service
class RoleCatalog(private val objectMapper: ObjectMapper) {

	fun roles(): List<RoleDefinition> {
		val definitions: Map<String, JsonNode> = readItems("seed/roles.json")
			.associateBy { it.path("code").asText() }
		val permissions: Map<String, PermissionDefinition> = readItems("seed/permissions.json")
			.associate { node ->
				node.path("code").asText() to PermissionDefinition(
					code = node.path("code").asText(),
					name = node.path("name").asText(),
					description = node.path("description").asText(),
				)
			}
		val grants: Map<String, List<String>> = readItems("seed/role_permissions.json")
			.associate { node ->
				node.path("role").asText() to node.path("permissions").toList().map { it.asText() }
			}

		return Role.entries.map { role ->
			val seed = definitions[role.code]
			RoleDefinition(
				code = role.code,
				name = seed?.path("name")?.asText()?.takeIf { it.isNotBlank() } ?: role.name,
				description = seed?.path("description")?.asText().orEmpty(),
				permissions = grants[role.code].orEmpty().mapNotNull { code -> permissions[code] },
			)
		}
	}

	private fun readItems(path: String): List<JsonNode> {
		val root = ClassPathResource(path).inputStream.use { objectMapper.readTree(it) }
		return root.path("items").toList()
	}
}
