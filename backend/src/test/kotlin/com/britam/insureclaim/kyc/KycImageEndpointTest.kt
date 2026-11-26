package com.britam.insureclaim.kyc

import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimService
import com.britam.insureclaim.common.GlobalExceptionHandler
import com.britam.insureclaim.role.Role
import com.britam.insureclaim.security.CurrentUserResolver
import com.britam.insureclaim.security.StorageProperties
import com.britam.insureclaim.storage.LocalFileStorageService
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserAccountService
import java.nio.file.Path
import java.util.Optional
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class KycImageEndpointTest {
	@TempDir
	lateinit var directory: Path

	@Test
	fun `review endpoint returns the uploaded front image`() {
		val storage = LocalFileStorageService(StorageProperties(uploadDir = directory.toString()))
		val bytes = byteArrayOf(1, 2, 3)
		val path = storage.store(MockMultipartFile("frontImage", "id.png", "image/png", bytes), "kyc", 1).storagePath
		val mvc = reviewEndpoint(storage, path)

		mvc.perform(get("/api/v1/admin/kyc/verifications/42/images/FRONT"))
			.andExpect(status().isOk)
			.andExpect(content().contentType("image/png"))
			.andExpect(content().bytes(bytes))
	}

	@Test
	fun `review endpoint explains when the stored file is missing`() {
		val storage = LocalFileStorageService(StorageProperties(uploadDir = directory.toString()))
		val mvc = reviewEndpoint(storage, "kyc/missing.png")

		mvc.perform(get("/api/v1/admin/kyc/verifications/42/images/FRONT"))
			.andExpect(status().isBadRequest)
			.andExpect(content().string(containsString("Stored file is no longer available")))
	}

	private fun reviewEndpoint(storage: LocalFileStorageService, imagePath: String) =
		MockMvcBuilders.standaloneSetup(
			KycReviewController(
				KycService(
					Mockito.mock(KycVerificationRepository::class.java).also {
						Mockito.`when`(it.findById(42L)).thenReturn(Optional.of(KycVerification(frontImagePath = imagePath)))
					},
					Mockito.mock(ClaimRepository::class.java),
					Mockito.mock(ClaimService::class.java),
					Mockito.mock(UserAccountService::class.java),
					storage,
				),
				Mockito.mock(UserAccountService::class.java).also {
					Mockito.`when`(it.requireUser(1L)).thenReturn(User(role = Role.AGENT.code))
				},
				Mockito.mock(CurrentUserResolver::class.java).also {
					Mockito.`when`(it.requireId()).thenReturn(1L)
				},
			),
		).setControllerAdvice(GlobalExceptionHandler()).build()
}
