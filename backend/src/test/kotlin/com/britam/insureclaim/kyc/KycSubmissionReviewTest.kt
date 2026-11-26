package com.britam.insureclaim.kyc

import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimService
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.role.Role
import com.britam.insureclaim.security.AuthenticatedUser
import com.britam.insureclaim.storage.LocalFileStorageService
import com.britam.insureclaim.storage.StoredFile
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserAccountService
import com.britam.insureclaim.user.UserRepository
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import org.mockito.Mockito
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(properties = [
	"spring.datasource.url=jdbc:h2:mem:kyc-review;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
	"spring.datasource.username=sa",
	"spring.datasource.password=",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.flyway.enabled=false",
	"insureclaim.seed.enabled=false",
])
@Transactional
class KycSubmissionReviewTest(
	@Autowired private val users: UserRepository,
	@Autowired private val verifications: KycVerificationRepository,
	@Autowired private val claims: ClaimRepository,
	@Autowired private val claimService: ClaimService,
	@Autowired private val accounts: UserAccountService,
	@Autowired private val context: WebApplicationContext,
) {
	@Test
	fun `screening flags remain pending until staff reject`() {
		val customer = customer()
		val (service, image) = serviceFor(customer)
		val submitted = service.submit(
			requireNotNull(customer.id),
			KycSubmissionRequest(KycDocumentType.NIC, "invalid", "Document Name", LocalDate.now().minusYears(10)),
			image, null, null,
		)

		assertEquals(KycStatus.PENDING, submitted.status)
		assertNotNull(submitted.failureReason)
		assertFalse(submitted.resubmissionAllowed)
		assertNull(submitted.verifiedAt)
		assertEquals(submitted.id, service.queue(null, 0, 20).content.single().id)
		assertEquals("Customer", service.queue(null, 0, 20).content.single().customerName)
		assertEquals("Document Name", submitted.fullNameOnDocument)
		assertEquals(1L, service.queue(null, 0, 1).totalElements)
		assertEquals(KycStatus.PENDING, service.latest(requireNotNull(customer.id))?.status)

		val reviewer = staff()
		val rejected = service.decide(submitted.id, reviewer, false, "Invalid document", true)
		assertEquals(KycStatus.REJECTED, rejected.status)
		assertEquals("Invalid document", rejected.failureReason)
		assertEquals(0L, service.queue(null, 0, 20).totalElements)
		assertFailsWith<ConflictException> { service.decide(submitted.id, reviewer, true, null, false) }

		val resubmitted = service.submit(
			requireNotNull(customer.id),
			KycSubmissionRequest(KycDocumentType.NIC, "123456789V", "Customer", LocalDate.now().minusYears(30)),
			image, null, null,
		)
		assertEquals(submitted.id, resubmitted.id)
		assertEquals(resubmitted.id, service.queue(null, 0, 20).content.single().id)
		assertEquals(1L, service.queue(null, 0, 1).totalElements)
	}

	@Test
	fun `valid submissions remain pending until staff approve`() {
		val customer = customer()
		val (service, image) = serviceFor(customer)
		val submitted = service.submit(
			requireNotNull(customer.id),
			KycSubmissionRequest(KycDocumentType.NIC, "123456789V", "Customer", LocalDate.now().minusYears(30)),
			image, null, null,
		)

		assertEquals(KycStatus.PENDING, submitted.status)
		assertFalse(submitted.stillValid)
		assertNull(submitted.expiresAt)
		assertEquals(submitted.id, service.queue(null, 0, 20).content.single().id)

		val approved = service.decide(submitted.id, staff(), true, null, false)
		assertEquals(KycStatus.VERIFIED, approved.status)
		assertNotNull(approved.expiresAt)
		assertEquals(0L, service.queue(null, 0, 20).totalElements)
	}

	@Test
	fun `full document number is available only to staff for pending review`() {
		val customer = customer()
		val reviewer = staff()
		val (service, image) = serviceFor(customer)
		val submitted = service.submit(
			requireNotNull(customer.id),
			KycSubmissionRequest(KycDocumentType.NIC, "123456789V", "Customer", LocalDate.now().minusYears(30)),
			image, null, null,
		)
		val mvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
		val staffPrincipal = AuthenticatedUser(requireNotNull(reviewer.id), reviewer.email, reviewer.role, reviewer.fullName)
		val customerPrincipal = AuthenticatedUser(requireNotNull(customer.id), customer.email, customer.role, customer.fullName)
		val reviewUrl = "/api/v1/admin/kyc/verifications/${submitted.id}/document-number"

		mvc.perform(get(reviewUrl).with(user(staffPrincipal)))
			.andExpect(status().isOk)
			.andExpect(header().string("Cache-Control", containsString("no-store")))
			.andExpect(content().json("""{"documentNumber":"123456789V"}"""))
		mvc.perform(get(reviewUrl).with(user(customerPrincipal)))
			.andExpect(status().isForbidden)
		mvc.perform(get("/api/v1/kyc/verifications/${submitted.id}").with(user(customerPrincipal)))
			.andExpect(status().isOk)
			.andExpect(content().string(containsString("******789V")))
			.andExpect(content().string(not(containsString("123456789V"))))
		mvc.perform(get("/api/v1/admin/kyc/verifications").with(user(staffPrincipal)))
			.andExpect(status().isOk)
			.andExpect(content().string(not(containsString("123456789V"))))
		assertFailsWith<ForbiddenException> { service.reviewDocumentNumber(submitted.id, customer) }

		service.decide(submitted.id, reviewer, false, "Cannot verify", true)
		mvc.perform(get(reviewUrl).with(user(staffPrincipal)))
			.andExpect(status().isConflict)
	}

	private fun customer() =
		users.save(User(email = "kyc-customer@example.com", fullName = "Customer", role = Role.CUSTOMER.code))

	private fun staff() =
		users.save(User(email = "kyc-staff@example.com", fullName = "Staff", role = Role.AGENT.code))

	private fun serviceFor(customer: User): Pair<KycService, MockMultipartFile> {
		val storage = Mockito.mock(LocalFileStorageService::class.java)
		val image = MockMultipartFile("frontImage", "front.png", "image/png", byteArrayOf(1))
		Mockito.`when`(storage.store(image, KycService.STORAGE_NAMESPACE, requireNotNull(customer.id)))
			.thenReturn(StoredFile("kyc/front.png", "front.png", "image/png", 1, "checksum"))
		return KycService(verifications, claims, claimService, accounts, storage) to image
	}
}
