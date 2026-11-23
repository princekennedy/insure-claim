package com.britam.insureclaim.vehicle

import com.britam.insureclaim.role.Role
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
	properties = [
		"spring.datasource.url=jdbc:h2:mem:vehicle-directory;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"insureclaim.seed.enabled=false",
	],
)
@Transactional
@WithMockUser(roles = ["AGENT"])
class VehicleAdminControllerTest(
	@Autowired private val users: UserRepository,
	@Autowired private val vehicles: VehicleRepository,
	@Autowired private val controller: VehicleAdminController,
) {
	@Test
	fun `directory excludes staff vehicles before pagination`() {
		val customer = users.save(User(email = "customer@example.com", fullName = "Customer", role = Role.CUSTOMER.code))
		val staff = users.save(User(email = "staff@example.com", fullName = "Staff", role = Role.AGENT.code))
		vehicles.save(Vehicle(owner = staff, registrationNumber = "AAA", make = "A", model = "A", year = 2020))
		vehicles.save(Vehicle(owner = customer, registrationNumber = "BBB", make = "B", model = "B", year = 2020))
		vehicles.save(Vehicle(owner = customer, registrationNumber = "CCC", make = "C", model = "C", year = 2020))

		val first = controller.list(0, 1)
		val second = controller.list(1, 1)
		assertEquals(2L, first.totalElements)
		assertEquals(2, first.totalPages)
		assertEquals("BBB", first.content.single().vehicle.registrationNumber)
		assertEquals(customer.id, first.content.single().customerId)
		assertEquals("CCC", second.content.single().vehicle.registrationNumber)
	}

	@Test
	fun `directory is empty when only staff own vehicles`() {
		val staff = users.save(User(email = "staff@example.com", fullName = "Staff", role = Role.AGENT.code))
		vehicles.save(Vehicle(owner = staff, registrationNumber = "AAA", make = "A", model = "A", year = 2020))

		val result = controller.list(0, 20)
		assertTrue(result.empty)
		assertEquals(0L, result.totalElements)
	}
}
