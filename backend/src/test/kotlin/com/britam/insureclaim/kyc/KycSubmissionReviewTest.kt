package com.britam.insureclaim.kyc

import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimService
import com.britam.insureclaim.role.Role
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
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.annotation.Transactional

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
) {
	@Test
	fun `screening flags remain pending until staff reject`() {
		val customer = customer()
		val (service, image) = serviceFor(customer)
		val submitted = service.submit(
			requireNotNull(customer.id),
			KycSubmissionRequest(KycDocumentType.NIC, "invalid", "Customer", LocalDate.now().minusYears(10)),
			image, null, null,
		)

		assertEquals(KycStatus.PENDING, submitted.status)
		assertNotNull(submitted.failureReason)
		assertFalse(submitted.resubmissionAllowed)
		assertNull(submitted.verifiedAt)
		assertEquals(submitted.id, service.queue(null, 0, 20).content.single().id)
		assertEquals(KycStatus.PENDING, service.latest(requireNotNull(customer.id))?.status)

		val rejected = service.decide(submitted.id, staff(), false, "Invalid document", true)
		assertEquals(KycStatus.REJECTED, rejected.status)
		assertEquals("Invalid document", rejected.failureReason)
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
