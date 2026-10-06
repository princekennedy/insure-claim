package com.britam.insureclaim.kyc

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Schema(name = "KycSubmission", description = "Identity details accompanying the uploaded document images")
data class KycSubmissionRequest(
	@field:NotNull(message = "documentType is required")
	val documentType: KycDocumentType,

	@field:NotBlank(message = "documentNumber is required")
	val documentNumber: String,

	@field:NotBlank(message = "fullNameOnDocument is required")
	val fullNameOnDocument: String,

	@field:Past(message = "dateOfBirth cannot be in the future")
	val dateOfBirth: LocalDate? = null,

	@Schema(description = "Claim that triggered this verification, when it is claim-driven")
	val claimId: Long? = null,
)

@Schema(name = "KycRejectionRequest")
data class KycRejectionRequest(
	@field:NotBlank(message = "reason is required")
	val reason: String,

	@Schema(description = "Let the customer correct the details and submit again")
	val resubmitAllowed: Boolean = true,
)

@Schema(name = "KycVerificationResponse")
data class KycVerificationResponse(
	val id: Long,
	val status: KycStatus,
	val statusLabel: String,
	val documentType: KycDocumentType,
	val documentLabel: String,
	@Schema(description = "Masked so staff views cannot leak a full identity number")
	val maskedDocumentNumber: String,
	val fullNameOnDocument: String,
	val dateOfBirth: LocalDate?,
	val confidenceScore: BigDecimal?,
	val failureReason: String?,
	val verifiedAt: Instant?,
	val verifiedBy: Long?,
	val expiresAt: LocalDate?,
	val stillValid: Boolean,
	val submittedAt: Instant,
	val updatedAt: Instant,
	val claimId: Long?,
	@Schema(description = "Staff left the customer free to send a corrected document")
	val resubmissionAllowed: Boolean,
	@Schema(description = "A new submission would replace this record")
	val reopenable: Boolean,
	val hasFrontImage: Boolean,
	val hasBackImage: Boolean,
	val hasSelfie: Boolean,
)

/** Image sides a reviewer can open; the mapping is deliberately narrow. */
enum class KycImageSide {
	FRONT,
	BACK,
	SELFIE,
}

fun KycVerification.toResponse(): KycVerificationResponse = KycVerificationResponse(
	id = id ?: 0L,
	status = status,
	statusLabel = status.label,
	documentType = documentType,
	documentLabel = documentType.displayLabel,
	maskedDocumentNumber = maskDocumentNumber(documentNumber),
	fullNameOnDocument = fullNameOnDoc,
	dateOfBirth = dateOfBirth,
	confidenceScore = confidenceScore,
	failureReason = failureReason,
	verifiedAt = verifiedAt,
	verifiedBy = verifiedBy,
	expiresAt = expiresAt,
	stillValid = isValidOn(),
	submittedAt = submittedAt,
	updatedAt = updatedAt,
	claimId = claim?.id,
	resubmissionAllowed = resubmissionAllowed,
	reopenable = reopenable(),
	hasFrontImage = frontImagePath != null,
	hasBackImage = backImagePath != null,
	hasSelfie = selfiePath != null,
)

private fun maskDocumentNumber(value: String): String = when {
	value.length <= 4 -> "*".repeat(value.length)
	else -> "*".repeat(value.length - 4) + value.takeLast(4)
}