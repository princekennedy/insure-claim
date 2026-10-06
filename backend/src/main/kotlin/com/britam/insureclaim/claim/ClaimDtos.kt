package com.britam.insureclaim.claim

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Schema(name = "FileClaimRequest", description = "Everything needed to submit a new claim")
data class FileClaimRequest(
	@field:NotNull val vehicleId: Long,
	@field:NotNull val incidentType: IncidentType,
	@field:NotNull
	@field:PastOrPresent(message = "incident date cannot be in the future")
	val incidentDate: LocalDate,
	@field:Size(max = 255) val incidentLocation: String? = null,
	@field:NotBlank @field:Size(min = 10, max = 4000) val description: String,
	@field:DecimalMin(value = "0.01") val estimatedAmount: BigDecimal? = null,
	val reportedByPolice: Boolean = false,
	val thirdPartyInvolved: Boolean = false,
	val requiresKyc: Boolean = true,
)

@Schema(name = "ClaimSummaryResponse", description = "Claim as shown in list views")
data class ClaimSummaryResponse(
	val id: Long,
	val claimNumber: String,
	val status: ClaimStatus,
	val statusLabel: String,
	val progressPercent: Int,
	val incidentType: IncidentType,
	val incidentDate: Instant,
	val vehicleRegistration: String,
	val vehicleLabel: String,
	val policyNumber: String,
	val estimatedAmount: BigDecimal?,
	val approvedAmount: BigDecimal?,
	val netSettlement: BigDecimal,
	val excessAmount: BigDecimal,
	val isFraudFlagged: Boolean,
	val requiresKyc: Boolean,
	val submittedAt: Instant,
	val settledAt: Instant?,
	val garageName: String? = null,
	val trackingToken: String? = null,
) {
	companion object {
		fun from(claim: Claim, garageName: String? = null, trackingToken: String? = null): ClaimSummaryResponse =
			ClaimSummaryResponse(
				id = claim.id ?: 0L,
				claimNumber = claim.claimNumber,
				status = claim.status,
				statusLabel = claim.status.isCustomerVisibleLabel,
				progressPercent = progressOf(claim.status),
				incidentType = claim.incidentType,
				incidentDate = claim.incidentDate,
				vehicleRegistration = claim.vehicle.registrationNumber,
				vehicleLabel = claim.vehicle.displayName(),
				policyNumber = claim.policy.policyNumber,
				estimatedAmount = claim.estimatedAmount,
				approvedAmount = claim.approvedAmount,
				netSettlement = claim.netSettlement(),
				excessAmount = claim.policy.excessAmount,
				isFraudFlagged = claim.isFraudFlagged,
				requiresKyc = claim.requiresKyc,
				submittedAt = claim.submittedAt,
				settledAt = claim.settledAt,
				garageName = garageName,
				trackingToken = trackingToken,
			)
	}
}

/** Progress shown on the customer tracker, derived from the state machine. */
fun progressOf(status: ClaimStatus): Int = when (status) {
	ClaimStatus.DRAFT -> 5
	ClaimStatus.SUBMITTED -> 15
	ClaimStatus.KYC_PENDING -> 25
	ClaimStatus.UNDER_REVIEW -> 40
	ClaimStatus.FRAUD_CHECK -> 50
	ClaimStatus.APPROVED -> 62
	ClaimStatus.GARAGE_ASSIGNED -> 72
	ClaimStatus.IN_REPAIR -> 82
	ClaimStatus.READY_FOR_PICKUP -> 92
	ClaimStatus.SETTLED -> 98
	ClaimStatus.CLOSED -> 100
	ClaimStatus.REJECTED -> 100
	ClaimStatus.WITHDRAWN -> 100
}

@Schema(name = "ClaimDetailResponse", description = "Full claim view including timeline and documents")
data class ClaimDetailResponse(
	val summary: ClaimSummaryResponse,
	val description: String,
	val incidentLocation: String?,
	val incidentLocalDate: LocalDate,
	val reportedByPolice: Boolean,
	val thirdPartyInvolved: Boolean,
	val fraudScore: Int,
	val customer: ClaimPartyResponse,
	val vehicle: ClaimVehicleResponse,
	val documents: List<ClaimDocumentResponse>,
	val timeline: List<ClaimTimelineEntry>,
	val nextStages: List<ClaimStageOption>,
	val repair: RepairJobSummaryResponse?,
	val fraudAlerts: List<FraudAlertSummary>,
	val kycStatus: String? = null,
	val canSubmitFeedback: Boolean,
	val createdAt: Instant,
	val updatedAt: Instant,
)

@Schema(name = "ClaimParty")
data class ClaimPartyResponse(
	val id: Long,
	val fullName: String,
	val email: String,
	val phone: String?,
)

@Schema(name = "ClaimVehicle")
data class ClaimVehicleResponse(
	val id: Long,
	val registrationNumber: String,
	val make: String,
	val model: String,
	val year: Int,
	val color: String?,
)

@Schema(name = "ClaimDocument")
data class ClaimDocumentResponse(
	val id: Long,
	val documentType: ClaimDocumentType,
	val fileName: String,
	val contentType: String,
	val sizeBytes: Long,
	val sizeLabel: String,
	val uploadedAt: Instant,
	val downloadUrl: String,
)

@Schema(name = "ClaimTimelineEntry")
data class ClaimTimelineEntry(
	val fromStatus: ClaimStatus?,
	val toStatus: ClaimStatus,
	val label: String,
	val note: String?,
	val actorLabel: String,
	val occurredAt: Instant,
	val isCurrent: Boolean,
)

@Schema(name = "ClaimStageOption", description = "Statuses the current user may move this claim to")
data class ClaimStageOption(
	val status: ClaimStatus,
	val label: String,
)

@Schema(name = "RepairJobSummary")
data class RepairJobSummaryResponse(
	val id: Long,
	val referenceCode: String,
	val status: String,
	val statusLabel: String,
	val progressPercent: Int,
	val garageId: Long,
	val garageName: String,
	val garagePhone: String,
	val garageAddress: String,
	val quotedAmount: BigDecimal?,
	val finalAmount: BigDecimal?,
	val estimatedDays: Int?,
	val warrantyDays: Int,
	val assignedAt: Instant,
	val startedAt: Instant?,
	val completedAt: Instant?,
	val notes: String?,
)

@Schema(name = "FraudAlertSummary")
data class FraudAlertSummary(
	val id: Long,
	val ruleCode: String,
	val category: String,
	val severity: String,
	val description: String,
	val status: String,
	val createdAt: Instant,
)

@Schema(name = "UpdateClaimStatusRequest", description = "Staff or customer status transition")
data class UpdateClaimStatusRequest(
	@field:NotNull val status: ClaimStatus,
	@field:Size(max = 500) val note: String? = null,
	@field:DecimalMin(value = "0.01") val approvedAmount: BigDecimal? = null,
	@field:DecimalMin(value = "0.00") val excessPaid: BigDecimal? = null,
	val rejectionReason: String? = null,
)

@Schema(name = "ClaimTrackingResponse", description = "Public claim tracking, no sign-in required")
data class ClaimTrackingResponse(
	val claimNumber: String,
	val status: ClaimStatus,
	val statusLabel: String,
	val progressPercent: Int,
	val incidentType: IncidentType,
	val incidentDate: Instant,
	val vehicleRegistration: String,
	val timeline: List<ClaimTimelineEntry>,
	val trackingTokenExpiresAt: Instant,
)

@Schema(name = "IncidentTypeCount")
data class IncidentTypeCount(
	val incidentType: IncidentType,
	val count: Long,
)
