package com.britam.insureclaim.garage

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimService
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.claim.UpdateClaimStatusRequest
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.security.GarageProperties
import com.britam.insureclaim.role.Role
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserAccountService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant

/**
 * Garage directory, repair-job lifecycle and post-repair feedback.
 *
 * Every method that maps a response is transactional on purpose: `open-in-view`
 * is disabled, so `claim.vehicle`, `job.garage` and `feedback.garage` are lazy
 * and cannot be touched once the session closes.
 */
@Service
class GarageService(
	private val garageRepository: GarageRepository,
	private val repairJobRepository: RepairJobRepository,
	private val claimRepository: ClaimRepository,
	private val claimService: ClaimService,
	private val accountService: UserAccountService,
	private val properties: GarageProperties,
) {

	companion object {
		private val log = LoggerFactory.getLogger(GarageService::class.java)
		private val random = SecureRandom()

		/** Reference the garage and customer both quote on the phone. */
		private const val REFERENCE_PREFIX = "RJ"
	}

	// ------------------------------------------------------------ directory ---

	@Transactional(readOnly = true)
	fun searchGarages(
		city: String?,
		query: String?,
		page: Int,
		size: Int,
	): PageResponse<GarageResponse> {
		val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
		return PageResponse.map(garageRepository.searchActive(city.trimOrNull(), query.trimOrNull(), pageable)) {
			GarageResponse.from(it)
		}
	}

	@Transactional(readOnly = true)
	fun getGarage(garageId: Long): GarageResponse {
		val garage = garageRepository.findById(garageId)
			.orElseThrow { NotFoundException("Garage", garageId) }
		return GarageResponse.from(garage)
	}

	/**
	 * Garages the insurer should act on: low ratings, complaints, slow
	 * turnaround or simply not finishing the work they accepted.
	 */
	@Transactional(readOnly = true)
	fun performanceWatchlist(): List<GaragePerformanceSummary> {
		val activeStatuses = RepairJobStatus.entries.filter { !it.isFinished }
		return garageRepository.findAll()
			.map { garage ->
				GaragePerformanceSummary(
					garageId = garage.id ?: 0L,
					garageName = garage.name,
					performanceStatus = garage.performanceStatus,
					performanceLabel = garage.performanceStatus.label,
					ratingAverage = garage.ratingAverage,
					ratingCount = garage.ratingCount,
					complaintCount = garage.complaintCount,
					jobsCompleted = garage.jobsCompleted,
					avgTurnaroundDays = garage.avgTurnaroundDays,
					activeJobs = repairJobRepository.countActiveForGarage(garage.id ?: 0L, activeStatuses),
					panelRating = garage.panelRating,
					flaggedAt = garage.lastScoredAt,
				)
			}
			.filter { it.performanceStatus != GaragePerformanceStatus.GOOD || it.activeJobs > 0 }
			.sortedWith(
				compareBy<GaragePerformanceSummary> { it.performanceStatus.ordinal }
					.thenByDescending { it.complaintCount }
					.thenBy { it.ratingAverage },
			)
	}

	// ---------------------------------------------------------- assignment ---

	/**
	 * Hands an approved claim to a panel garage. The claim and the repair job
	 * move together so the customer never sees one without the other.
	 */
	@Transactional
	fun assignGarage(claimId: Long, request: AssignGarageRequest, actor: User): RepairJobResponse {
		requireStaff(actor, "Only insurer staff can assign a garage")

		val claim = claimRepository.findByIdWithDetails(claimId)
			.orElseThrow { NotFoundException("Claim", claimId) }

		if (claim.status != ClaimStatus.APPROVED) {
			throw BusinessRuleException(
				"A garage can only be assigned once the claim is approved (currently ${claim.status.isCustomerVisibleLabel.lowercase()})",
				"CLAIM_NOT_APPROVED",
			)
		}
		if (repairJobRepository.findByClaimId(claimId).isPresent) {
			throw ConflictException("This claim already has a garage assigned", "GARAGE_ALREADY_ASSIGNED")
		}

		val garage = garageRepository.findById(request.garageId)
			.orElseThrow { NotFoundException("Garage", request.garageId) }
		if (!garage.isAcceptingWork) {
			throw BusinessRuleException(
				"${garage.name} is ${garage.performanceStatus.label.lowercase()} and cannot take new work",
				"GARAGE_NOT_ACCEPTING_WORK",
			)
		}

		val job = RepairJob(
			claim = claim,
			garage = garage,
			referenceCode = nextReferenceCode(),
			status = RepairJobStatus.ASSIGNED,
			quotedAmount = request.quotedAmount,
			approvedAmount = request.approvedAmount ?: request.quotedAmount,
			estimatedDays = request.estimatedDays,
			warrantyDays = request.warrantyDays,
			notes = request.notes?.trim()?.takeIf { it.isNotEmpty() },				assignedAt = Instant.now(),
			)

	fun com.britam.insureclaim.user.User.isStaff(): Boolean = this.role?.isStaff() ?: false

	fun com.britam.insureclaim.user.User.canViewAllClaims(): Boolean = this.role?.canViewAllClaims() ?: false

		val saved = repairJobRepository.save(job)

		val moved = claimService.transition(
			claim = claim,
			target = ClaimStatus.GARAGE_ASSIGNED,
			request = UpdateClaimStatusRequest(
				status = ClaimStatus.GARAGE_ASSIGNED,
				note = "Repair assigned to ${garage.name} (job ${saved.referenceCode}).",
			),
			actor = actor,
		)

		log.info(
			"Claim {} assigned to garage {} as job {} by {}",
			moved.claimNumber, garage.code, saved.referenceCode, actor.email,
		)
		return saved.toResponse(canSubmitFeedback = feedbackAllowed(moved))
	}

	@Transactional(readOnly = true)
	fun jobForClaim(claimId: Long, actor: User): RepairJobResponse? {
		val claim = claimRepository.findByIdWithDetails(claimId)
			.orElseThrow { NotFoundException("Claim", claimId) }
		if (!actor.role.canViewAllClaims() && claim.customer.id != actor.id) {
			throw ForbiddenException("You do not have access to this claim")
		}
		return repairJobRepository.findByClaimId(claimId)
			.orElse(null)
			?.toResponse(canSubmitFeedback = feedbackAllowed(claim))
	}

	@Transactional(readOnly = true)
	fun getJob(jobId: Long, actor: User): RepairJobResponse {
		val job = requireJob(jobId)
		val claimCustomerId = job.claim.customer.id
		if (!actor.role.canViewAllClaims() && claimCustomerId != actor.id) {
			throw ForbiddenException("You do not have access to this repair job")
		}
		return job.toResponse(canSubmitFeedback = feedbackAllowed(job.claim))
	}

	@Transactional(readOnly = true)
	fun listJobs(
		garageId: Long?,
		status: RepairJobStatus?,
		actor: User,
		page: Int,
		size: Int,
	): PageResponse<RepairJobResponse> {
		val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
		val resolvedGarageId = garageId
			?: if (actor.role.isStaff()) null else throw ForbiddenException("Customers cannot list repair jobs")
		val statuses = if (status != null) {
			setOf(status)
		} else {
			RepairJobStatus.entries.filter { !it.isFinished }.toSet()
		}

		val source = repairJobRepository.search(resolvedGarageId, statuses, pageable)
		return PageResponse(
			content = source.content.map { it.toResponse(canSubmitFeedback = false) },
			page = source.number,
			size = source.size,
			totalElements = source.totalElements,
			totalPages = source.totalPages,
			first = source.isFirst,
			last = source.isLast,
			empty = source.isEmpty,
		)
	}

	// ------------------------------------------------------------- lifecycle --

	/**
	 * Moves the repair along and keeps the claim in step. Turnaround and job
	 * counters are refreshed here so garage scoring never lags the data.
	 */
	@Transactional
	fun updateJob(jobId: Long, request: UpdateRepairJobRequest, actor: User): RepairJobResponse {
		requireStaff(actor, "Only insurer staff can update a repair job")

		val job = requireJob(jobId)
		val from = job.status
		if (from == request.status) {
			throw ConflictException("The repair is already ${from.label.lowercase()}", "STATUS_UNCHANGED")
		}
		if (!RepairJobStatus.canTransitionTo(from, request.status)) {
			throw BusinessRuleException(
				"A repair cannot move from ${from.label.lowercase()} to ${request.status.label.lowercase()}",
				"INVALID_REPAIR_TRANSITION",
			)
		}

		val now = Instant.now()
		job.status = request.status
		request.finalAmount?.let { job.finalAmount = it }
		request.notes?.trim()?.takeIf { it.isNotEmpty() }?.let { job.notes = it }
		if (request.status == RepairJobStatus.IN_REPAIR && job.startedAt == null) {
			job.startedAt = now
		}
		if (request.status in setOf(RepairJobStatus.COMPLETED, RepairJobStatus.DELIVERED) && job.completedAt == null) {
			job.completedAt = now
		}
		if (request.finalAmount != null && request.status != RepairJobStatus.DELIVERED) {
			throw BusinessRuleException("The final amount is confirmed on delivery", "FINAL_AMOUNT_NOT_DUE")
		}

		val saved = repairJobRepository.save(job)
		val claim = saved.claim

		val implied = request.status.impliedClaimStatus
		if (implied != null && claim.status != implied && ClaimStatus.canTransitionTo(claim.status, implied)) {
			claimService.transition(
				claim = claim,
				target = implied,
				request = UpdateClaimStatusRequest(
					status = implied,
					note = "Repair moved to ${request.status.label.lowercase()}.",
				),
				actor = actor,
			)
		}

		refreshGarageScore(saved.garage)
		log.info("Repair job {} moved {} -> {} by {}", saved.referenceCode, from, request.status, actor.email)
		return saved.toResponse(canSubmitFeedback = feedbackAllowed(saved.claim))
	}

	@Transactional
	fun cancelJob(jobId: Long, reason: String?, actor: User): RepairJobResponse {
		requireStaff(actor, "Only insurer staff can cancel a repair job")

		val job = requireJob(jobId)
		if (job.status.isFinished) {
			throw ConflictException("This repair is already ${job.status.label.lowercase()}", "REPAIR_ALREADY_FINISHED")
		}
		if (!RepairJobStatus.canTransitionTo(job.status, RepairJobStatus.CANCELLED)) {
			throw BusinessRuleException(
				"A ${job.status.label.lowercase()} repair can no longer be cancelled",
				"INVALID_REPAIR_TRANSITION",
			)
		}

		job.status = RepairJobStatus.CANCELLED
		job.notes = listOfNotNull(job.notes, reason?.trim()?.takeIf { it.isNotEmpty() }?.let { "Cancelled: $it" })
			.joinToString(" ")
			.take(500)
		val saved = repairJobRepository.save(job)

		refreshGarageScore(saved.garage)
		log.info("Repair job {} cancelled by {}", saved.referenceCode, actor.email)
		return saved.toResponse(canSubmitFeedback = false)
	}

	// --------------------------------------------------------------- helpers --

	private fun requireStaff(actor: User, message: String) {
		if (!actor.role.isStaff()) throw ForbiddenException(message)
	}

	private fun requireJob(jobId: Long): RepairJob =
		repairJobRepository.findByIdWithDetails(jobId)
			.orElseThrow { NotFoundException("Repair job", jobId) }

	/** Feedback unlocks once the vehicle is back with the customer. */
	private fun feedbackAllowed(claim: Claim): Boolean =
		claim.status == ClaimStatus.SETTLED || claim.status == ClaimStatus.CLOSED

	/**
	 * Recomputes the rolling garage score from the jobs it has actually run.
	 * Ratings are handled separately by the feedback service.
	 */
	@Transactional
	fun refreshGarageScore(garage: Garage) {
		val since = Instant.now().minus(Duration.ofDays(properties.performanceWindowDays.toLong()))
		val completed = repairJobRepository.findCompletedSince(since)
			.filter { it.garage.id == garage.id }

		garage.jobsCompleted = repairJobRepository.countByGarageIdAndStatus(
			garage.id ?: 0L,
			RepairJobStatus.DELIVERED,
		).toInt()
		garage.recordTurnaround(completed.mapNotNull(RepairJob::turnaround))
		garage.lastScoredAt = Instant.now()
		garage.performanceStatus = scoreGarage(garage)
		garageRepository.save(garage)
	}

	/**
	 * Thresholds are deliberately conservative: a garage is only punished for
	 * sustained, evidence-backed weakness so one bad job cannot suspend a panel.
	 * All of them live in `insureclaim.garage` so the insurer can tune them
	 * without a redeploy.
	 */
	internal fun scoreGarage(garage: Garage): GaragePerformanceStatus {
		if (!garage.active) return GaragePerformanceStatus.SUSPENDED

		val complaints = garage.complaintCount
		val rating = garage.ratingAverage
		val turnaround = garage.avgTurnaroundDays

		return when {
			complaints >= properties.suspensionComplaintThreshold ->
				GaragePerformanceStatus.SUSPENDED

			complaints >= properties.underperformingComplaintThreshold ||
				(ratingCountMeaningful(garage) && rating < properties.underperformingRatingThreshold) ||
				(turnaround != null && turnaround > properties.slowTurnaroundDays) ->
				GaragePerformanceStatus.UNDERPERFORMING

			complaints >= 1 ||
				(ratingCountMeaningful(garage) && rating < properties.watchRatingThreshold) ->
				GaragePerformanceStatus.WATCH

			else -> GaragePerformanceStatus.GOOD
		}
	}

	/** A 1-star average off a single review is noise, not a trend. */
	private fun ratingCountMeaningful(garage: Garage): Boolean =
		garage.ratingCount >= properties.minimumRatingCount

	private fun nextReferenceCode(): String {
		while (true) {
			val suffix = random.nextInt(0, 100_000_000).toString().padStart(8, '0')
			val code = "$REFERENCE_PREFIX-$suffix"
			if (repairJobRepository.findByReferenceCode(code).isEmpty()) return code
		}
	}

	internal fun RepairJob.toResponse(canSubmitFeedback: Boolean): RepairJobResponse = RepairJobResponse(
		id = id ?: 0L,
		referenceCode = referenceCode,
		claimId = claim.id ?: 0L,
		claimNumber = claim.claimNumber,
		status = status,
		statusLabel = status.label,
		progressPercent = progressPercent(),
		garage = GarageResponse.from(garage),
		quotedAmount = quotedAmount,
		approvedAmount = approvedAmount,
		finalAmount = finalAmount,
		assignedAt = assignedAt,
		startedAt = startedAt,
		completedAt = completedAt,
		estimatedDays = estimatedDays,
		warrantyDays = warrantyDays,
		notes = notes,
		turnaroundDays = turnaround()?.toDays()?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP),
		canSubmitFeedback = canSubmitFeedback,
	)

	private fun String?.trimOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun com.britam.insureclaim.user.User.isStaff(): Boolean = this.role?.isStaff() ?: false

fun com.britam.insureclaim.user.User.canViewAllClaims(): Boolean = this.role?.canViewAllClaims() ?: false
}