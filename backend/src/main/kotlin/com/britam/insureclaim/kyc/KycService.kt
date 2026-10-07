package com.britam.insureclaim.kyc

import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimService
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.claim.UpdateClaimStatusRequest
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.common.ValidationException
import com.britam.insureclaim.storage.LocalFileStorageService
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserAccountService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Service
@Transactional
class KycService(
	private val kycRepository: KycVerificationRepository,
	private val claimRepository: ClaimRepository,
	private val claimService: ClaimService,
	private val accountService: UserAccountService,
	private val storageService: LocalFileStorageService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	private val minimumClaimantAge = 18

	fun submit(
		userId: Long,
		request: KycSubmissionRequest,
		frontImage: MultipartFile?,
		backImage: MultipartFile?,
		selfie: MultipartFile?,
	): KycVerificationResponse {
		val customer = accountService.requireCustomer(userId)
		if (customer.isStaff()) {
			throw BusinessRuleException("Staff accounts do not submit identity documents", "STAFF_CANNOT_SUBMIT_KYC")
		}
		if (frontImage == null) {
			throw ValidationException(
				"A photo of the front of your identity document is required",
				"FRONT_IMAGE_REQUIRED",
			)
		}

		val documentNumber = request.documentNumber.trim().uppercase()
		val fullName = request.fullNameOnDocument.trim()

		val existing = kycRepository.findByUserIdAndDocumentType(userId, request.documentType).orElse(null)
		if (existing != null && existing.status.isBlocking) {
			throw ConflictException(
				"You already have a ${request.documentType.displayLabel.lowercase()} awaiting review (reference #${existing.id})",
				"KYC_ALREADY_IN_REVIEW",
			)
		}
		if (existing != null && !existing.reopenable()) {
			throw ConflictException(
				"Your ${request.documentType.displayLabel.lowercase()} was rejected and cannot be resubmitted. Contact support.",
				"KYC_RESUBMISSION_NOT_ALLOWED",
			)
		}

		val claim = request.claimId?.let { claimId ->
			val found = claimRepository.findById(claimId).orElseThrow { NotFoundException("Claim", claimId) }
			if (found.customer.id != userId) {
				throw ForbiddenException("This claim belongs to another customer")
			}
			found
		}

		val screening = applyAutomatedScreening(
			user = customer,
			documentType = request.documentType,
			documentNumber = documentNumber,
			fullName = fullName,
			dateOfBirth = request.dateOfBirth,
		)

		val verification = existing ?: KycVerification().apply {
			this.user = customer
		}
		verification.apply {
			this.user = customer
			this.claim = claim
			this.documentType = request.documentType
			this.documentNumber = documentNumber
			this.fullNameOnDoc = fullName
			this.dateOfBirth = request.dateOfBirth
			this.frontImagePath = storageService.store(frontImage, STORAGE_NAMESPACE, userId).storagePath
			this.backImagePath = backImage?.let { storageService.store(it, STORAGE_NAMESPACE, userId).storagePath }
			this.selfiePath = selfie?.let { storageService.store(it, STORAGE_NAMESPACE, userId).storagePath }
			this.status = screening.status
			this.failureReason = screening.reason
			this.confidenceScore = screening.confidence
			this.expiresAt = screening.expiresAt
			this.resubmissionAllowed = screening.status == KycStatus.REJECTED
			this.verifiedBy = null
			this.verifiedAt = null
		}
		val saved = kycRepository.save(verification)

		if (saved.status == KycStatus.VERIFIED) {
			releaseClaimsAwaitingKyc(saved.user)
		}
		log.info("KYC {} submitted for user {} and screened as {}", saved.id, userId, saved.status)
		return saved.toResponse()
	}

	@Transactional(readOnly = true)
	fun history(userId: Long): List<KycVerificationResponse> =
		kycRepository.findByUserIdOrderBySubmittedAtDesc(userId)
			.map(KycVerification::toResponse)

	@Transactional(readOnly = true)
	fun latest(userId: Long): KycVerificationResponse? =
		kycRepository.findFirstByUserIdOrderBySubmittedAtDesc(userId)
			.map(KycVerification::toResponse)
			.orElse(null)

	@Transactional(readOnly = true)
	fun getOwn(userId: Long, verificationId: Long): KycVerificationResponse {
		val verification = kycRepository.findById(verificationId)
			.orElseThrow { NotFoundException("KYC verification", verificationId) }
		if (verification.user.id != userId) {
			throw ForbiddenException("This verification belongs to another customer")
		}
		return verification.toResponse()
	}

	fun decide(
		verificationId: Long,
		reviewer: User,
		approved: Boolean,
		reason: String?,
		resubmitAllowed: Boolean,
	): KycVerificationResponse {
		if (!reviewer.isStaff()) {
			throw ForbiddenException("Only insurer staff can decide a verification")
		}
		val verification = kycRepository.findById(verificationId)
			.orElseThrow { NotFoundException("KYC verification", verificationId) }
		if (verification.status.isSettled) {
			throw ConflictException(
				"This verification is already ${verification.status.label.lowercase()}",
				"KYC_ALREADY_SETTLED",
			)
		}

		val now = Instant.now()
		verification.verifiedBy = reviewer.id
		verification.verifiedAt = now

		if (approved) {
			verification.status = KycStatus.VERIFIED
			verification.failureReason = null
			verification.resubmissionAllowed = false
			verification.expiresAt = verification.effectiveExpiry()
			verification.confidenceScore = maxOf(
				verification.confidenceScore ?: BigDecimal.ZERO,
				MANUAL_APPROVAL_CONFIDENCE,
			)
			releaseClaimsAwaitingKyc(verification.user)
		} else {
			verification.status = KycStatus.REJECTED
			verification.resubmissionAllowed = resubmitAllowed
			verification.failureReason = reason?.trim()?.takeIf { it.isNotEmpty() }
				?: "Documents could not be verified by the insurer"
			verification.confidenceScore = BigDecimal.ZERO
			verification.expiresAt = null
		}

		val saved = kycRepository.save(verification)
		log.info("KYC {} {} by {}", saved.id, if (approved) "approved" else "rejected", reviewer.email)
		return saved.toResponse()
	}

	@Transactional(readOnly = true)
	fun queue(status: KycStatus?, page: Int, size: Int): PageResponse<KycVerificationResponse> {
		val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
		val effectiveStatus = status ?: KycStatus.PENDING
		return PageResponse.map(kycRepository.findByStatusOrderBySubmittedAtAsc(effectiveStatus, pageable)) {
			it.toResponse()
		}
	}

	fun image(verificationId: Long, side: KycImageSide, reviewer: User): ImagePayload {
		if (!reviewer.isStaff()) {
			throw ForbiddenException("Only insurer staff can open identity document images")
		}
		val verification = kycRepository.findById(verificationId)
			.orElseThrow { NotFoundException("KYC verification", verificationId) }
		val path = when (side) {
			KycImageSide.FRONT -> verification.frontImagePath
			KycImageSide.BACK -> verification.backImagePath
			KycImageSide.SELFIE -> verification.selfiePath
		} ?: throw NotFoundException(side.name.lowercase(), verificationId)

		return ImagePayload(
			contentType = imageContentType(path),
			bytes = storageService.load(path),
		)
	}

	private data class Screening(
		val status: KycStatus,
		val reason: String?,
		val confidence: BigDecimal,
		val expiresAt: LocalDate?,
	)

	private fun applyAutomatedScreening(
		user: User,
		documentType: KycDocumentType,
		documentNumber: String,
		fullName: String,
		dateOfBirth: LocalDate?,
	): Screening {
		val candidate = KycVerification()
		candidate.user = user
		candidate.documentType = documentType
		candidate.documentNumber = documentNumber
		candidate.fullNameOnDoc = fullName
		candidate.dateOfBirth = dateOfBirth

		if (!candidate.structurallyValidNumber()) {
			return Screening(
				status = KycStatus.REJECTED,
				reason = "The ${documentType.displayLabel.lowercase()} number is not a valid format",
				confidence = BigDecimal.ZERO,
				expiresAt = null,
			)
		}

		if (dateOfBirth != null &&
			dateOfBirth.plusYears(minimumClaimantAge.toLong()).isAfter(LocalDate.now())
		) {
			return Screening(
				status = KycStatus.REJECTED,
				reason = "The policyholder must be at least $minimumClaimantAge years old",
				confidence = BigDecimal.ZERO,
				expiresAt = null,
			)
		}

		if (fullName.isBlank() || user.nic.isNullOrBlank()) {
			return Screening(KycStatus.VERIFIED, null, STRUCTURAL_PASS_CONFIDENCE, expiryFor(documentType))
		}

		if (!candidate.nameMatchesAccount(user.fullName)) {
			return Screening(
				status = KycStatus.PENDING,
				reason = "Document name '$fullName' does not match the account holder",
				confidence = NAME_MISMATCH_CONFIDENCE,
				expiresAt = null,
			)
		}

		return Screening(KycStatus.VERIFIED, null, STRUCTURAL_PASS_CONFIDENCE, expiryFor(documentType))
	}

	private fun releaseClaimsAwaitingKyc(customer: User) {
		val customerId = customer.id ?: return
		val waiting = claimRepository.findByCustomerIdAndStatusInOrderBySubmittedAtDesc(
			customerId,
			listOf(ClaimStatus.KYC_PENDING),
			PageRequest.of(0, MAX_RELEASED_CLAIMS),
		)
		if (waiting.isEmpty) return
		val systemActor = User().apply {
			email = SYSTEM_ACTOR_EMAIL
			passwordHash = ""
			fullName = "Digital KYC"
			role = "ADMIN"
		}
		waiting.forEach { claim ->
			claimService.transition(
				claim = claim,
				target = ClaimStatus.UNDER_REVIEW,
				request = UpdateClaimStatusRequest(
					status = ClaimStatus.UNDER_REVIEW,
					note = "Identity verified digitally; assessment can begin.",
				),
				actor = systemActor,
			)
		}
		log.info("Released {} claim(s) from the KYC gate for user {}", waiting.size, customerId)
	}

	private fun expiryFor(documentType: KycDocumentType, from: LocalDate = LocalDate.now()): LocalDate =
		from.plusYears(documentType.validityYears.toLong())

	private fun imageContentType(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
		"png" -> "image/png"
		"webp" -> "image/webp"
		"heic" -> "image/heic"
		"pdf" -> "application/pdf"
		else -> "image/jpeg"
	}

	data class ImagePayload(val contentType: String, val bytes: ByteArray)

	companion object {
		const val STORAGE_NAMESPACE = "kyc"

		private const val SYSTEM_ACTOR_EMAIL = "system@insureclaim.local"
		private const val MAX_RELEASED_CLAIMS = 50

		private val STRUCTURAL_PASS_CONFIDENCE = BigDecimal("0.9400")
		private val NAME_MISMATCH_CONFIDENCE = BigDecimal("0.3500")
		private val MANUAL_APPROVAL_CONFIDENCE = BigDecimal("0.9900")
	}
}
