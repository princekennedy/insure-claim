package com.britam.insureclaim.chatbot

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimDetailResponse
import com.britam.insureclaim.claim.ClaimPartyResponse
import com.britam.insureclaim.claim.ClaimStageOption
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.claim.ClaimSummaryResponse
import com.britam.insureclaim.claim.ClaimTimelineEntry
import com.britam.insureclaim.claim.ClaimVehicleResponse
import com.britam.insureclaim.claim.IncidentType
import com.britam.insureclaim.policy.Policy
import com.britam.insureclaim.user.User
import com.britam.insureclaim.vehicle.Vehicle
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatClaimContextBuilderTest {

	@Test
	fun `extracts a standard claim number`() {
		assertEquals(
			"CLM-2026-AB12CD",
			ChatClaimContextBuilder.extractClaimNumber("My claim is CLM-2026-AB12CD, please check."),
		)
	}

	@Test
	fun `normalises lowercase and spaces`() {
		assertEquals(
			"CLM-2026-AB12CD",
			ChatClaimContextBuilder.extractClaimNumber("clm 2026 ab12cd"),
		)
	}

	@Test
	fun `returns null when no claim number is present`() {
		assertNull(ChatClaimContextBuilder.extractClaimNumber("Where is my payout?"))
	}

	@Test
	fun `does not treat ordinary reference text as a claim number`() {
		assertNull(ChatClaimContextBuilder.extractClaimNumber("Policy POL-2026-AB12CD"))
	}

	@Test
	fun `builds prompt facts and a customer reply from verified claim data`() {
		val instant = Instant.parse("2026-01-12T09:00:00Z")
		val context = ChatClaimContextBuilder.build(sampleClaim(instant), sampleDetail(instant))

		assertContains(context.promptBlock, "CLM-2026-AB12CD")
		assertContains(context.promptBlock, "Jane Doe")
		assertContains(context.promptBlock, "KES 120,000.00")
		assertContains(context.promptBlock, "KDA 123A")
		assertContains(context.promptBlock, "Toyota Corolla (2020)")
		assertContains(context.promptBlock, "KYC verification: Verified")

		assertContains(context.fallbackReply, "Jane Doe")
		assertContains(context.fallbackReply, "Approved")
		assertContains(context.fallbackReply, "Next step: Under review")
	}

	private fun sampleClaim(instant: Instant): Claim {
		val customer = User(
			email = "jane@example.com",
			fullName = "Jane Doe",
			phone = "+254700000000",
		).apply { id = 7L }
		val vehicle = Vehicle(
			owner = customer,
			registrationNumber = "KDA 123A",
			make = "Toyota",
			model = "Corolla",
			year = 2020,
			color = "White",
		).apply { id = 3L }
		val policy = Policy(
			policyNumber = "POL-2026-0001",
			customer = customer,
			vehicle = vehicle,
			startDate = LocalDate.of(2025, 1, 1),
			endDate = LocalDate.of(2026, 1, 1),
			premiumAmount = BigDecimal("45000.00"),
			sumInsured = BigDecimal("1200000.00"),
			excessAmount = BigDecimal("5000.00"),
		).apply { id = 2L }
		return Claim(
			claimNumber = "CLM-2026-AB12CD",
			policy = policy,
			customer = customer,
			vehicle = vehicle,
			incidentType = IncidentType.ACCIDENT,
			incidentDate = instant,
			incidentLocation = "Nairobi",
			description = "Rear-ended at a traffic light.",
			estimatedAmount = BigDecimal("130000.00"),
			approvedAmount = BigDecimal("120000.00"),
			status = ClaimStatus.APPROVED,
		).apply { id = 1L }
	}

	private fun sampleDetail(instant: Instant): ClaimDetailResponse {
		val claim = sampleClaim(instant)
		return ClaimDetailResponse(
			summary = ClaimSummaryResponse.from(claim),
			description = claim.description,
			incidentLocation = claim.incidentLocation,
			incidentLocalDate = claim.incidentLocalDate,
			reportedByPolice = true,
			thirdPartyInvolved = false,
			fraudScore = 0,
			customer = ClaimPartyResponse(
				id = 7L,
				fullName = "Jane Doe",
				email = "",
				phone = null,
			),
			vehicle = ClaimVehicleResponse(
				id = 3L,
				registrationNumber = "KDA 123A",
				make = "Toyota",
				model = "Corolla",
				year = 2020,
				color = "White",
			),
			documents = emptyList(),
			timeline = listOf(
				ClaimTimelineEntry(
					fromStatus = ClaimStatus.SUBMITTED,
					toStatus = ClaimStatus.APPROVED,
					label = "Approved",
					note = "Assessor approved the repair.",
					actorLabel = "Claims team",
					occurredAt = instant,
					isCurrent = true,
				),
			),
			nextStages = listOf(ClaimStageOption(ClaimStatus.UNDER_REVIEW, "Under review")),
			repair = null,
			fraudAlerts = emptyList(),
			kycStatus = "Verified",
			canSubmitFeedback = false,
			createdAt = instant,
			updatedAt = instant,
		)
	}
}
