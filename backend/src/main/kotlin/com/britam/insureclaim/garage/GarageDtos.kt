package com.britam.insureclaim.garage

import com.britam.insureclaim.feedback.GarageFeedback
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

@Schema(name = "GarageResponse", description = "Panel garage as shown to customers and staff")
data class GarageResponse(
	val id: Long,
	val code: String,
	val name: String,
	val address: String,
	val city: String,
	val contactPhone: String,
	val contactEmail: String?,
	val panelRating: BigDecimal?,
	@Schema(description = "Mean of every customer rating, 0.00 when unrated")
	val ratingAverage: BigDecimal,
	val ratingCount: Int,
	val complaintCount: Int,
	val jobsCompleted: Int,
	val avgTurnaroundDays: BigDecimal?,
	val isPanelGarage: Boolean,
	val active: Boolean,
	val performanceStatus: GaragePerformanceStatus,
	val performanceLabel: String,
	val acceptingWork: Boolean,
) {
	companion object {
		fun from(garage: Garage): GarageResponse = GarageResponse(
			id = garage.id ?: 0L,
			code = garage.code,
			name = garage.name,
			address = garage.address,
			city = garage.city,
			contactPhone = garage.contactPhone,
			contactEmail = garage.contactEmail,
			panelRating = garage.panelRating,
			ratingAverage = garage.ratingAverage,
			ratingCount = garage.ratingCount,
			complaintCount = garage.complaintCount,
			jobsCompleted = garage.jobsCompleted,
			avgTurnaroundDays = garage.avgTurnaroundDays,
			isPanelGarage = garage.isPanelGarage,
			active = garage.active,
			performanceStatus = garage.performanceStatus,
			performanceLabel = garage.performanceStatus.label,
			acceptingWork = garage.isAcceptingWork,
		)
	}
}

@Schema(name = "RepairJobResponse", description = "Repair job as shown to the customer and insurer staff")
data class RepairJobResponse(
	val id: Long,
	val referenceCode: String,
	val claimId: Long,
	val claimNumber: String,
	val status: RepairJobStatus,
	val statusLabel: String,
	val progressPercent: Int,
	val garage: GarageResponse,
	val quotedAmount: BigDecimal?,
	val approvedAmount: BigDecimal?,
	val finalAmount: BigDecimal?,
	val assignedAt: Instant,
	val startedAt: Instant?,
	val completedAt: Instant?,
	val estimatedDays: Int?,
	val warrantyDays: Int,
	val notes: String?,
	@Schema(description = "Days from work starting to completion, null until the repair finishes")
	val turnaroundDays: BigDecimal?,
	val canSubmitFeedback: Boolean,
)

@Schema(name = "AssignGarageRequest")
data class AssignGarageRequest(
	@field:NotNull(message = "garageId is required")
	@field:Positive(message = "garageId must be positive")
	val garageId: Long,

	@field:DecimalMin(value = "0.01", message = "quotedAmount must be greater than zero")
	val quotedAmount: BigDecimal? = null,

	@field:DecimalMin(value = "0.01", message = "approvedAmount must be greater than zero")
	val approvedAmount: BigDecimal? = null,

	@field:Min(value = 1, message = "estimatedDays must be at least 1")
	@field:Max(value = 365, message = "estimatedDays must be a year or less")
	val estimatedDays: Int? = null,

	@field:Size(max = 500, message = "notes must be 500 characters or fewer")
	val notes: String? = null,

	@field:Min(value = 0, message = "warrantyDays cannot be negative")
	@field:Max(value = 730, message = "warrantyDays must be two years or less")
	val warrantyDays: Int = 90,
)

@Schema(name = "UpdateRepairJobRequest")
data class UpdateRepairJobRequest(
	@field:NotNull(message = "status is required")
	val status: RepairJobStatus,

	@field:DecimalMin(value = "0.01", message = "finalAmount must be greater than zero")
	val finalAmount: BigDecimal? = null,

	@field:Size(max = 500, message = "notes must be 500 characters or fewer")
	val notes: String? = null,
)

@Schema(name = "GarageFeedbackRequest", description = "Customer rating of a completed repair")
data class GarageFeedbackRequest(
	@field:NotNull(message = "overallRating is required")
	@field:DecimalMin(value = "1", message = "overallRating must be between 1 and 5")
	@field:DecimalMax(value = "5", message = "overallRating must be between 1 and 5")
	val overallRating: BigDecimal,

	val qualityRating: BigDecimal? = null,
	val timelinessRating: BigDecimal? = null,
	val priceFairnessRating: BigDecimal? = null,
	val staffCourtesyRating: BigDecimal? = null,

	@field:Size(max = 1000, message = "comments must be 1000 characters or fewer")
	val comments: String? = null,

	val recommendAgain: Boolean? = null,
)

@Schema(name = "GarageFeedbackResponse")
data class GarageFeedbackResponse(
	val id: Long,
	val claimId: Long,
	val claimNumber: String,
	val garageId: Long,
	val garageName: String,
	@Schema(description = "Comments are hidden from other customers but visible to insurer staff")
	val comments: String?,
	val overallRating: BigDecimal,
	val qualityRating: BigDecimal?,
	val timelinessRating: BigDecimal?,
	val priceFairnessRating: BigDecimal?,
	val staffCourtesyRating: BigDecimal?,
	val recommendAgain: Boolean?,
	@Schema(description = "Low score or an explicit no; drives the garage watchlist")
	val isComplaint: Boolean,
	val createdAt: Instant,
	val commentVisible: Boolean,
)

@Schema(name = "GaragePerformanceSummary")
data class GaragePerformanceSummary(
	val garageId: Long,
	val garageName: String,
	val performanceStatus: GaragePerformanceStatus,
	val performanceLabel: String,
	val ratingAverage: BigDecimal,
	val ratingCount: Int,
	val complaintCount: Int,
	val jobsCompleted: Int,
	val avgTurnaroundDays: BigDecimal?,
	val activeJobs: Long,
	val panelRating: BigDecimal?,
	val flaggedAt: Instant?,
)

/** Customer-facing view: ratings and turnaround, no identifying feedback. */
fun GarageFeedback.toPublicResponse(): GarageFeedbackResponse = toResponse(commentVisible = false)

/** Staff view: comments are the whole point of the triage queue. */
fun GarageFeedback.toStaffResponse(): GarageFeedbackResponse = toResponse(commentVisible = true)

private fun GarageFeedback.toResponse(commentVisible: Boolean): GarageFeedbackResponse = GarageFeedbackResponse(
	id = id ?: 0L,
	claimId = claim.id ?: 0L,
	claimNumber = claim.claimNumber,
	garageId = garage.id ?: 0L,
	garageName = garage.name,
	comments = comments.takeIf { commentVisible },
	overallRating = overallRating,
	qualityRating = qualityRating,
	timelinessRating = timelinessRating,
	priceFairnessRating = priceFairnessRating,
	staffCourtesyRating = staffCourtesyRating,
	recommendAgain = recommendAgain,
	isComplaint = isComplaint,
	createdAt = createdAt,
	commentVisible = commentVisible,
)