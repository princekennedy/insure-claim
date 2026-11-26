package com.britam.insureclaim.user

import com.britam.insureclaim.role.RoleEntity
import com.britam.insureclaim.role.RoleRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(properties = [
	"spring.datasource.url=jdbc:h2:mem:user-creation;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.flyway.enabled=false",
	"insureclaim.seed.enabled=false",
])
@Transactional
class UserCreationSecurityTest(
	@Autowired private val context: WebApplicationContext,
	@Autowired private val roles: RoleRepository,
	@Autowired private val users: UserRepository,
) {
	@Test
	fun `administrators create customers officers and managers while other roles are forbidden`() {
		listOf("CUSTOMER", "OFFICER", "MANAGER").forEach { roles.save(RoleEntity(code = it, name = it)) }
		val mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()

		for (role in listOf("CUSTOMER", "OFFICER", "MANAGER")) {
			val email = "${role.lowercase()}@example.com"
			mvc.perform(
				post("/api/v1/users")
					.with(user("insurer-admin").roles("INSURER_ADMIN"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"email":"$email","password":"StrongPass1!","fullName":"Test User","role":"$role"}"""),
			).andExpect(status().isCreated)
			assertEquals(role, users.findByEmailIgnoreCase(email).orElseThrow().role)
		}

		mvc.perform(
			post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"email":"admin-created@example.com","password":"StrongPass1!","fullName":"Test User","role":"MANAGER"}"""),
		).andExpect(status().isCreated)

		for (actorRole in listOf("MANAGER", "OFFICER", "AGENT", "CUSTOMER")) {
			mvc.perform(
				post("/api/v1/users")
					.with(user("other").roles(actorRole))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"email":"forbidden@example.com","password":"StrongPass1!","fullName":"Test User","role":"CUSTOMER"}"""),
			).andExpect(status().isForbidden)
		}
	}
}
