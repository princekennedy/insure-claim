package com.britam.insureclaim.claim

import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.fraud.FraudDetectionService
import com.britam.insureclaim.garage.RepairJob
import com.britam.insureclaim.garage.RepairJobRepository
import com.britam.insureclaim.garage.RepairJobStatus
import com.britam.insureclaim.kyc.KycDocumentType
import com.britam.insureclaim.kyc.KycStatus
import com.britam.insureclaim.kyc.KycVerificationRepository
import com.britam.insureclaim.policy.PolicyStatus
import com.britam.insureclaim.security.ClaimProperties
import com.britam.insureclaim.storage.LocalFileStorageService
import com.britam.insureclaim.storage.StoredFile
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserAccountService
import com.britam.insureclaim.vehicle.Vehicle
import com.britam.insureclaim.vehicle.VehicleRepository
import jakarta.persistence.criteria.Predicate
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Base64

/**
 * Claim lifecycle orchestration: submission, tracking, evidence, status
 * transitions and public tracking links.
 */
@Service
@Transactional
class ClaimService(
	private val claimRepository: ClaimRepository,
	private val claimDocumentRepository: ClaimDocumentRepository,
	private val statusEventRepository: ClaimStatusEventRepository,
	private val publicTokenRepository: ClaimPublicTokenRepository,
	private val vehicleRepository: VehicleRepository,
	private val repairJobRepository: RepairJobRepository,
	private val kycRepository: KycVerificationRepository,
	private val accountService: UserAccountService,
	private val fraudDetectionService: FraudDetectionService,
	private val storageService: LocalFileStorageService,
	private val properties: ClaimProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)
	private val secureRandom = SecureRandom()

	/**
	 * Validates cover, files the claim and immediately screens it for fraud.
	 *
	 * The claim is only accepted when the vehicle has an in-force policy whose
	 * window covers the incident date â€” that single check is what stops the
	 * majority of bogus submissions.
	 */
	fun fileClaim(customer: User, request: FileClaimRequest): Claim {
		val vehicle = vehicleRepository.findOwnedBy(customer.id ?: 0L, request.vehicleId)
			.orElseThrow { NotFoundException("Vehicle", request.vehicleId) }
		val policy = accountService.requirePolicyForVehicle(customer.id ?: 0L, vehicle.id ?: 0L)

		val effectiveStatus = PolicyStatus.effective(policy.status, policy.endDate)
		if (effectiveStatus != PolicyStatus.ACTIVE) {
			throw BusinessRuleException(
				"Policy ${policy.policyNumber} expired on ${policy.endDate}. Cover cannot be claimed against an expired policy.",
				"POLICY_NOT_ACTIVE",
			)
		}
		if (!policy.coversIncidentDate(request.incidentDate)) {
			throw BusinessRuleException(
				"The incident date ${request.incidentDate} falls outside the policy period " +
					"(${policy.startDate} to ${policy.endDate})",
				"INCIDENT_OUTSIDE_POLICY_PERIOD",
			)
		}
		if (request.incidentType.requiresPoliceReport && !request.reportedByPolice) {
			throw BusinessRuleException(
				"A police report is mandatory for ${request.incidentType.name.lowercase()} claims",
				"POLICE_REPORT_REQUIRED",
			)
		}

		// Notification window: motor policies in LK require notification within 7 days.
		val daysSinceIncident = java.time.temporal.ChronoUnit.DAYS.between(request.incidentDate, LocalDate.now())
		if (daysSinceIncident > 14) {
			log.info("Customer {} filed a claim {} days after the incident", customer.id, daysSinceIncident)
		}

		val openCount = claimRepository.findByCustomerIdAndStatusInOrderBySubmittedAtDesc(
			customer.id ?: 0L,
			ClaimStatus.entries.filter { it.isOpen },
			PageRequest.of(0, 1),
		).totalElements
		if (openCount >= properties.maxOpenClaimsPerCustomer) {
			throw BusinessRuleException(
				"You already have $openCount open claims. The limit is ${properties.maxOpenClaimsPerCustomer}; " +
					"contact us before filing another.",
				"TOO_MANY_OPEN_CLAIMS",
			)
		}

		val now = Instant.now()
		val claim = Claim(
			claimNumber = generateClaimNumber(),
			policy = policy,
			customer = customer,
			vehicle = vehicle,
			incidentType = request.incidentType,
			incidentDate = request.incidentDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
			incidentLocation = request.incidentLocation?.trim(),
			description = request.description.trim(),
			estimatedAmount = request.estimatedAmount,
			requiresKyc = request.requiresKyc,
			reportedByPolice = request.reportedByPolice,
			thirdPartyInvolved = request.thirdPartyInvolved,
			status = ClaimStatus.SUBMITTED,
			submittedAt = now,
		)

		val initialStatus = if (claim.requiresKyc && !hasVerifiedKyc(customer)) ClaimStatus.KYC_PENDING else ClaimStatus.SUBMITTED
		claim.status = initialStatus
		claim.addStatusEvent(
			from = null,
			to = initialStatus,
			note = if (initialStatus == ClaimStatus.KYC_PENDING) {
				"Claim submitted. Identity verification is required before assessment."
			} else {
				"Claim submitted successfully."
			},
			actorId = customer.id,
			actorLabel = customer.fullNameOrEmail(),
		)

		val saved = claimRepository.save(claim)
		persistTimeline(saved)
		fraudDetectionService.evaluate(saved)

		issueTrackingToken(saved)
		log.info("Filed claim {} for customer {}", saved.claimNumber, customer.id)
		return saved
	}

	// ------------------------------------------------------------ reading ----

	@Transactional(readOnly = true)
	fun findAccessible(claimId: Long, actor: User): Claim {
		val claim = claimRepository.findByIdWithDetails(claimId)
			.orElseThrow { NotFoundException("Claim", claimId) }
		if (!actor.role.canViewAllClaims() && claim.customer.id != actor.id) {
			// Deliberately a 403 rather than a 404 so the API does not become a
			// claim-existence oracle for other customers' ids.
			throw ForbiddenException("You do not have access to this claim")
		}
		return claim
	}

	fun findByNumber(claimNumber: String, actor: User): Claim {
		val claim = claimRepository.findByClaimNumber(claimNumber.trim().uppercase())
			.orElseThrow { NotFoundException("Claim", claimNumber) }
		if (!actor.role.canViewAllClaims() && claim.customer.id != actor.id) {
			throw ForbiddenException("You do not have access to this claim")
		}
		return claim
	}

	/**
	 * Single filtered entry point used by both the customer list (scoped to their
	 * own claims) and the staff queue (optionally across all customers).
	 */
	@Transactional(readOnly = true)
	fun search(
		actor: User,
		statuses: Set<ClaimStatus>?,
		incidentTypes: Set<IncidentType>?,
		fraudOnly: Boolean,
		query: String?,
		from: LocalDate?,
		to: LocalDate?,
		page: Int,
		size: Int,
	): PageResponse<ClaimSummaryResponse> {
		val scopedToCustomer = !actor.role.canViewAllClaims()
		val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by("submittedAt").descending())

		val spec = Specification<Claim> { root, _, cb ->
			val customerJoin = root.join<Any, Any>("customer")
			val vehicleJoin = root.join<Any, Vehicle>("vehicle")
			val predicates = mutableListOf<Predicate>()

			if (scopedToCustomer) {
				predicates += cb.equal(customerJoin.get<Any>("id"), actor.id)
			}
			if (!statuses.isNullOrEmpty()) {
				predicates += root.get<ClaimStatus>("status").`in`(statuses)
			}
			if (!incidentTypes.isNullOrEmpty()) {
				predicates += root.get<IncidentType>("incidentType").`in`(incidentTypes)
			}
			if (fraudOnly) {
				predicates += cb.isTrue(root.get("isFraudFlagged"))
			}
			if (!query.isNullOrBlank()) {
				val pattern = "%${query.trim().lowercase()}%"
				predicates += cb.or(
					cb.like(cb.lower(root.get<String>("claimNumber")), pattern),
					cb.like(cb.lower(vehicleJoin.get("registrationNumber")), pattern),
					cb.like(cb.lower(root.get<String>("description")), pattern),
				)
			}
			if (from != null) {
				predicates += cb.greaterThanOrEqualTo(
					root.get("incidentDate"),
					from.atStartOfDay().atZone(ZoneOffset.UTC).toInstant(),
				)
			}
			if (to != null) {
				predicates += cb.lessThan(
					root.get("incidentDate"),
					to.plusDays(1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant(),
				)
			}
			cb.and(*predicates.toTypedArray())
		}

		// Map inside this transaction: the claim's vehicle and repair job are lazy,
		// and `open-in-view` is off.
		return pageResponse(claimRepository.findAll(spec, pageable), !scopedToCustomer)
	}

	// -------------------------------------------------------- transitions ----

	/**
	 * Moves a claim to a new status, recording the change on the timeline and
	 * driving the follow-on work (KYC gate, fraud screening, settlement).
	 */
	fun transition(claim: Claim, target: ClaimStatus, request: UpdateClaimStatusRequest, actor: User): Claim {
		val from = claim.status

		if (from == target) {
			throw ConflictException("Claim is already ${target.isCustomerVisibleLabel}", "STATUS_UNCHANGED")
		}
		if (!ClaimStatus.canTransitionTo(from, target)) {
			throw BusinessRuleException(
				"A claim cannot move from ${from.isCustomerVisibleLabel} to ${target.isCustomerVisibleLabel}",
				"ILLEGAL_STATUS_TRANSITION",
			)
		}
		if (!actor.role.isStaff() && target !in ClaimStatus.CUSTOMER_TRANSITIONS[from].orEmpty()) {
			throw ForbiddenException("You cannot move this claim to ${target.isCustomerVisibleLabel}")
		}

		when (target) {
			ClaimStatus.APPROVED -> applyApproval(claim, request)
			ClaimStatus.REJECTED -> applyRejection(claim, request)
			ClaimStatus.KYC_PENDING -> claim.requiresKyc = true
			ClaimStatus.FRAUD_CHECK -> fraudDetectionService.evaluate(claim)
			ClaimStatus.READY_FOR_PICKUP -> requireCompletedRepair(claim)
			ClaimStatus.SETTLED -> applySettlement(claim, actor)
			ClaimStatus.WITHDRAWN -> {
				// A withdrawn claim must not leave an open repair job behind.
				activeRepairJob(claim)?.let { job ->
					job.status = RepairJobStatus.CANCELLED
					repairJobRepository.save(job)
				}
			}
			else -> Unit
		}

		claim.status = target
		val note = request.note ?: request.rejectionReason ?: defaultNoteFor(target, actor)
		val customerVisible = target != ClaimStatus.FRAUD_CHECK
		claim.addStatusEvent(
			from = from,
			to = target,
			note = note,
			actorId = actor.id,
			actorLabel = actor.fullNameOrEmail(),
			visibleToCustomer = customerVisible,
		)
		val saved = claimRepository.save(claim)
		persistTimeline(saved)
		log.info("Claim {} moved {} -> {} by {}", saved.claimNumber, from, target, actor.email)
		return saved
	}

	private fun applyApproval(claim: Claim, request: UpdateClaimStatusRequest) {
		if (claim.isFraudFlagged) {
			throw BusinessRuleException(
				"This claim is flagged for fraud review and cannot be approved until the alert is cleared",
				"FRAUD_FLAG_BLOCKS_APPROVAL",
			)
		}
		val amount = request.approvedAmount
			?: claim.estimatedAmount
			?: throw BusinessRuleException("An approved amount is required to approve a claim", "APPROVED_AMOUNT_REQUIRED")
		claim.approvedAmount = amount
		request.excessPaid?.let { claim.excessPaid = it }
		if (claim.approvedAmount!! > claim.policy.sumInsured) {
			throw BusinessRuleException(
				"Approved amount cannot exceed the insured value ${claim.policy.sumInsured}",
				"APPROVED_AMOUNT_EXCEEDS_SUM_INSURED",
			)
		}
	}

	private fun applyRejection(claim: Claim, request: UpdateClaimStatusRequest) {
		request.rejectionReason?.takeIf { it.isNotBlank() } ?: throw BusinessRuleException(
			"A rejection reason must be recorded",
			"REJECTION_REASON_REQUIRED",
		)
	}

	private fun requireCompletedRepair(claim: Claim) {
		val job = activeRepairJob(claim)
			?: throw BusinessRuleException("No garage has been assigned to this claim yet", "NO_GARAGE_ASSIGNED")
		if (job.status != RepairJobStatus.COMPLETED && job.status != RepairJobStatus.QUALITY_CHECK) {
			throw BusinessRuleException(
				"The repair is still ${job.status.label.lowercase()}. Only a completed repair can be marked ready for pickup.",
				"REPAIR_NOT_COMPLETE",
			)
		}
	}

	private fun applySettlement(claim: Claim, actor: User) {
		if (claim.approvedAmount == null) {
			throw BusinessRuleException("A claim must be approved before it can be settled", "NOT_APPROVED")
		}
		val job = activeRepairJob(claim)
		if (job == null || job.status != RepairJobStatus.DELIVERED) {
			throw BusinessRuleException(
				"The vehicle must be delivered by the garage before the claim is settled",
				"VEHICLE_NOT_DELIVERED",
			)
		}
		job.finalAmount = job.finalAmount ?: job.approvedAmount ?: claim.approvedAmount
		repairJobRepository.save(job)
		claim.settledAt = Instant.now()
		log.info("Claim {} settled at {} by {}", claim.claimNumber, claim.netSettlement(), actor.email)
	}

	private fun defaultNoteFor(target: ClaimStatus, actor: User): String = when (target) {
		ClaimStatus.SUBMITTED -> "Claim submitted."
		ClaimStatus.KYC_PENDING -> "Identity verification required before assessment can begin."
		ClaimStatus.UNDER_REVIEW -> "Assessment started by ${actor.fullNameOrEmail()}."
		ClaimStatus.FRAUD_CHECK -> "Claim routed to automated risk screening."
		ClaimStatus.APPROVED -> "Claim approved."
		ClaimStatus.REJECTED -> "Claim rejected."
		ClaimStatus.GARAGE_ASSIGNED -> "Repair garage assigned."
		ClaimStatus.IN_REPAIR -> "Repair work started."
		ClaimStatus.READY_FOR_PICKUP -> "Repair completed. Vehicle ready for collection."
		ClaimStatus.SETTLED -> "Claim settled."
		ClaimStatus.CLOSED -> "Claim closed."
		ClaimStatus.WITHDRAWN -> "Claim withdrawn by the customer."
		ClaimStatus.DRAFT -> "Claim saved as a draft."
	}

	// ----------------------------------------------------------- evidence ----

	fun addDocument(
		claim: Claim,
		file: MultipartFile,
		documentType: ClaimDocumentType,
		actor: User,
	): ClaimDocument {
		if (file.isEmpty) {
			throw BusinessRuleException("The uploaded file is empty", "EMPTY_FILE")
		}
		val stored: StoredFile = storageService.store(file, STORAGE_NAMESPACE, claim.id ?: 0L)

		val document = ClaimDocument(
			claim = claim,
			documentType = documentType,
			fileName = stored.originalFileName,
			contentType = stored.contentType,
			sizeBytes = stored.sizeBytes,
			storagePath = stored.storagePath,
			checksum = stored.checksum,
		)
		val saved = claimDocumentRepository.save(document)
		claim.addDocument(saved)

		claim.addStatusEvent(
			from = claim.status,
			to = claim.status,
			note = "${documentType.name.lowercase().replace('_', ' ')} uploaded (${saved.fileName})",
			actorId = actor.id,
			actorLabel = actor.fullNameOrEmail(),
			visibleToCustomer = true,
		)
		claimRepository.save(claim)
		persistTimeline(claim)

		// New evidence can clear or raise a screening signal.
		fraudDetectionService.evaluate(claim)
		return saved
	}

	fun deleteDocument(claim: Claim, documentId: Long, actor: User): ClaimDocument {
		val document = claimDocumentRepository.findById(documentId)
			.orElseThrow { NotFoundException("Document", documentId) }
		if (document.claim.id != claim.id) {
			throw ForbiddenException("This document belongs to a different claim")
		}
		if (document.documentType == ClaimDocumentType.POLICE_REPORT && claim.status.isTerminal) {
			throw BusinessRuleException(
				"Evidence cannot be removed from a closed claim",
				"CLAIM_CLOSED",
			)
		}
		storageService.delete(document.storagePath)
		claimDocumentRepository.delete(document)
		claim.removeDocument(document)
		claimRepository.save(claim)
		fraudDetectionService.evaluate(claim)
		return document
	}

	// ------------------------------------------------------- tracking link ----

	/**
	 * Issues (or reuses) a public tracking token. Callers use this to build the
	 * "track your claim without an account" link they can share or bookmark.
	 */
	fun issueTrackingToken(claim: Claim): String {
		val existing = publicTokenRepository.findByClaimId(claim.id ?: 0L).orElse(null)
		if (existing != null) {
			if (existing.isUsable()) return existing.token
			publicTokenRepository.delete(existing)
		}
		val token = randomToken()
		publicTokenRepository.save(
			ClaimPublicToken(
				claimId = claim.id ?: 0L,
				claim = claim,
				token = token,
				expiresAt = Instant.now().plus(Duration.ofDays(properties.settlementWindowDays.toLong())),
			),
		)
		return token
	}

	@Transactional(readOnly = true)
	fun trackByToken(token: String): Claim {
		val record = publicTokenRepository.findByToken(token.trim())
			.orElseThrow { NotFoundException("Tracking link", "not found or expired") }
		if (!record.isUsable()) {
			publicTokenRepository.delete(record)
			throw NotFoundException("Tracking link", "not found or expired")
		}
		return record.claim
	}

	// -------------------------------------------------------- projections ----

	@Transactional(readOnly = true)
	fun toDetail(claim: Claim, actor: User): ClaimDetailResponse {
		val customerVisibleTimeline = statusEventRepository
			.findByClaimIdAndVisibleToCustomerTrueOrderByOccurredAtAsc(claim.id ?: 0L)
		val timeline = customerVisibleTimeline.mapIndexed { index, event ->
			ClaimTimelineEntry(
				fromStatus = event.fromStatus,
				toStatus = event.toStatus,
				label = event.toStatus.isCustomerVisibleLabel,
				note = event.note,
				actorLabel = event.actorLabel,
				occurredAt = event.occurredAt,
				isCurrent = index == customerVisibleTimeline.lastIndex,
			)
		}

		val repair = activeRepairJob(claim)?.let(::toRepairSummary)
		val allowedTargets = if (actor.role.isStaff()) {
			ClaimStatus.allowedNextStates(claim.status)
		} else {
			ClaimStatus.CUSTOMER_TRANSITIONS[claim.status].orEmpty()
		}

		return ClaimDetailResponse(
			summary = toSummary(claim),
			description = claim.description,
			incidentLocation = claim.incidentLocation,
			incidentLocalDate = claim.incidentLocalDate,
			reportedByPolice = claim.reportedByPolice,
			thirdPartyInvolved = claim.thirdPartyInvolved,
			fraudScore = if (actor.role.isStaff()) claim.fraudScore else 0,
			customer = ClaimPartyResponse(
				id = claim.customer.id ?: 0L,
				fullName = claim.customer.fullName,
				email = if (actor.role.isStaff()) claim.customer.email else "",
				phone = if (actor.role.isStaff()) claim.customer.phone else null,
			),
			vehicle = ClaimVehicleResponse(
				id = claim.vehicle.id ?: 0L,
				registrationNumber = claim.vehicle.registrationNumber,
				make = claim.vehicle.make,
				model = claim.vehicle.model,
				year = claim.vehicle.year,
				color = claim.vehicle.color,
			),
			documents = claimDocumentRepository.findByClaimIdOrderByUploadedAtDesc(claim.id ?: 0L)
				.map(ClaimDocument::toResponse),
			timeline = timeline,
			nextStages = allowedTargets
				.filter { it != claim.status }
				.map { ClaimStageOption(it, it.isCustomerVisibleLabel) },
			repair = repair,
			fraudAlerts = if (actor.role.isStaff()) {
				fraudDetectionService.alertsForClaim(claim.id ?: 0L)
			} else {
				emptyList()
			},
			kycStatus = claimKycStatus(claim),
			canSubmitFeedback = claim.status == ClaimStatus.SETTLED ||
				claim.status == ClaimStatus.CLOSED ||
				claim.status == ClaimStatus.READY_FOR_PICKUP,
			createdAt = claim.createdAt,
			updatedAt = claim.updatedAt,
		)
	}

	@Transactional(readOnly = true)
	fun toSummary(claim: Claim, garageName: String? = null, includeTrackingToken: Boolean = false): ClaimSummaryResponse {
		val name = garageName ?: activeRepairJob(claim)?.garage?.name
		val token = if (includeTrackingToken) {
			publicTokenRepository.findByClaimId(claim.id ?: 0L).map { it.token }.orElse(null)
		} else {
			null
		}
		return ClaimSummaryResponse.from(claim, garageName = name, trackingToken = token)
	}

	fun toRepairSummary(job: RepairJob): RepairJobSummaryResponse = RepairJobSummaryResponse(
		id = job.id ?: 0L,
		referenceCode = job.referenceCode,
		status = job.status.name,
		statusLabel = job.status.label,
		progressPercent = job.progressPercent(),
		garageId = job.garage.id ?: 0L,
		garageName = job.garage.name,
		garagePhone = job.garage.contactPhone,
		garageAddress = "${job.garage.address}, ${job.garage.city}",
		quotedAmount = job.quotedAmount,
		finalAmount = job.finalAmount,
		estimatedDays = job.estimatedDays,
		warrantyDays = job.warrantyDays,
		assignedAt = job.assignedAt,
		startedAt = job.startedAt,
		completedAt = job.completedAt,
		notes = job.notes,
	)

	/**
	 * `open-in-view` is off, so every read that touches a lazy proxy (vehicle,
	 * repair job, documents) has to be mapped while the session is still open.
	 */
	@Transactional(readOnly = true)
	fun pageResponse(page: Page<Claim>, includeTokens: Boolean = false): PageResponse<ClaimSummaryResponse> {
		// One batched lookup for the whole page instead of a query per claim.
		val garageNames = repairJobRepository
			.findByClaimIds(page.content.mapNotNull { it.id })
			.associate { (it.claim.id ?: 0L) to it.garage.name }
		return PageResponse(
			content = page.content.map { toSummary(it, garageNames[it.id ?: 0L], includeTokens) },
			page = page.number,
			size = page.size,
			totalElements = page.totalElements,
			totalPages = page.totalPages,
			first = page.isFirst,
			last = page.isLast,
			empty = page.isEmpty,
		)
	}

	// ------------------------------------------------------------ helpers ----

	@Transactional(readOnly = true)
	fun activeRepairJob(claim: Claim): RepairJob? =
		repairJobRepository.findByClaimId(claim.id ?: 0L).orElse(null)

	fun hasVerifiedKyc(user: User): Boolean =
		kycRepository.existsByUserIdAndStatus(user.id ?: 0L, KycStatus.VERIFIED)

	fun claimKycStatus(claim: Claim): String? =
		kycRepository.findByUserIdAndDocumentType(claim.customer.id ?: 0L, KycDocumentType.NIC)
			.map { it.status.name }
			.orElse(null)

	/**
	 * Hibernate only cascades from the owning side, and the timeline is appended
	 * after the claim is saved, so events are written explicitly.
	 */
	private fun persistTimeline(claim: Claim) {
		claim.statusEvents.forEach { event ->
			if (event.id == null) {
				event.claim = claim
				statusEventRepository.save(event)
			}
		}
	}

	private fun generateClaimNumber(): String {
		var candidate: String
		do {
			val suffix = ByteArray(4).also { secureRandom.nextBytes(it) }
			val code = Base64.getUrlEncoder().withoutPadding().encodeToString(suffix).take(6).uppercase()
			candidate = "CLM-${LocalDate.now().year}-$code"
		} while (claimRepository.existsByClaimNumber(candidate))
		return candidate
	}

	private fun randomToken(): String {
		val bytes = ByteArray(24).also { secureRandom.nextBytes(it) }
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
	}

	companion object {
		const val STORAGE_NAMESPACE = "claims"
	}
}
/** Public projection of an uploaded document, shared with the claim controller. */
fun ClaimDocument.toResponse(): ClaimDocumentResponse = ClaimDocumentResponse(
	id = id ?: 0L,
	documentType = documentType,
	fileName = fileName,
	contentType = contentType,
	sizeBytes = sizeBytes,
	sizeLabel = sizeLabel,
	uploadedAt = uploadedAt,
	downloadUrl = "/api/v1/claims/${claim.id}/documents/$id",
)
