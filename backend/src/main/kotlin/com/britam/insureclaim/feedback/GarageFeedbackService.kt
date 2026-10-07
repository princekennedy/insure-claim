package com.britam.insureclaim.feedback

import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ConflictException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.PageResponse
import com.britam.insureclaim.garage.Garage
import com.britam.insureclaim.garage.GarageFeedbackRequest
import com.britam.insureclaim.garage.GarageFeedbackResponse
import com.britam.insureclaim.garage.GarageRepository
import com.britam.insureclaim.garage.GarageService
import com.britam.insureclaim.garage.RepairJobRepository
import com.britam.insureclaim.garage.toPublicResponse
import com.britam.insureclaim.garage.toStaffResponse
import com.britam.insureclaim.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class GarageFeedbackService(
	private val feedbackRepository: GarageFeedbackRepository,
	private val garageRepository: GarageRepository,
	private val repairJobRepository: RepairJobRepository,
	private val claimRepository: ClaimRepository,
	private val garageService: GarageService,
) {
	companion object {
		private val log = LoggerFactory.getLogger(GarageFeedbackService::class.java)
	}

	private val complaintRating = BigDecimal("2.00")

	@Transactional
	fun submit(claimId: Long, request: GarageFeedbackRequest, actor: User): GarageFeedbackResponse {
		val claim = claimRepository.findByIdWithDetails(claimId)
			.orElseThrow { NotFoundException("Claim", claimId) }
		if (claim.customer.id != actor.id && !actor.isStaff()) {
			throw ForbiddenException("Only the policyholder can rate this repair")
		}
		if (feedbackRepository.existsByClaimId(claimId)) {
			throw ConflictException("You have already reviewed this repair", "FEEDBACK_ALREADY_SUBMITTED")
		}

		val job = repairJobRepository.findByClaimId(claimId).orElse(null)
			?: throw BusinessRuleException(
				"This claim was not repaired at a panel garage, so there is nothing to rate",
				"NO_REPAIR_JOB",
			)
		if (claim.status != ClaimStatus.SETTLED && claim.status != ClaimStatus.CLOSED) {
			throw BusinessRuleException(
				"You can rate the garage once you have collected the vehicle",
				"CLAIM_NOT_SETTLED",
			)
		}

		val feedback = GarageFeedback(
			claim = claim,
			garage = job.garage,
			repairJob = job,
			customer = actor,
			overallRating = request.overallRating,
			qualityRating = request.qualityRating,
			timelinessRating = request.timelinessRating,
			priceFairnessRating = request.priceFairnessRating,
			staffCourtesyRating = request.staffCourtesyRating,
			comments = request.comments?.trim()?.takeIf { it.isNotEmpty() },
			recommendAgain = request.recommendAgain,
		)
		val saved = feedbackRepository.save(feedback)
		recomputeGarage(job.garage)

		log.info(
			"Feedback {} recorded for garage {} (overall {}) by {}",
			saved.id, job.garage.code, saved.overallRating, actor.email,
		)
		return saved.toStaffResponse()
	}

	@Transactional(readOnly = true)
	fun myFeedback(userId: Long, page: Int, size: Int): PageResponse<GarageFeedbackResponse> {
		val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
		return PageResponse.map(feedbackRepository.findByCustomerWithGarage(userId, pageable)) {
			it.toStaffResponse()
		}
	}

	@Transactional(readOnly = true)
	fun forGarage(
		garageId: Long,
		actor: User,
		page: Int,
		size: Int,
	): PageResponse<GarageFeedbackResponse> {
		garageRepository.findById(garageId).orElseThrow { NotFoundException("Garage", garageId) }
		val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
		val page1 = feedbackRepository.findByGarageIdOrderByCreatedAtDesc(garageId, pageable)
		val asStaff = actor.isStaff()
		return PageResponse(
			content = page1.content.map { if (asStaff) it.toStaffResponse() else it.toPublicResponse() },
			page = page1.number,
			size = page1.size,
			totalElements = page1.totalElements,
			totalPages = page1.totalPages,
			first = page1.isFirst,
			last = page1.isLast,
			empty = page1.isEmpty,
		)
	}

	@Transactional
	fun recomputeGarage(garage: Garage) {
		val row = feedbackRepository.ratingSummaryForGarage(garage.id ?: 0L).firstOrNull() as? Array<*>
		val sum = row?.get(0) as? BigDecimal ?: BigDecimal.ZERO
		val count = (row?.get(1) as? Number)?.toInt() ?: 0

		garage.recomputeRating(sum, count)
		garage.complaintCount = feedbackRepository.countComplaintsForGarage(garage.id ?: 0L, complaintRating)
		garageRepository.save(garage)
		garageService.refreshGarageScore(garage)
	}
}
