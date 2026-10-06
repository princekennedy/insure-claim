package com.britam.insureclaim.fraud

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.claim.FraudAlertSummary
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.policy.PolicyStatus
import com.britam.insureclaim.security.FraudProperties
import com.britam.insureclaim.user.User
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/** One rule hit, produced while scoring a claim. */
data class FraudSignal(
	val ruleCode: String,
	val category: FraudCategory,
	val severity: FraudSeverity,
	val description: String,
)

/**
 * Rule-based fraud screening.
 *
 * Each rule contributes a severity weight to a 0-100 score; the claim's score is
 * the capped sum of the weights of every rule that fired. Alerts are keyed on
 * (claim, rule) so re-running the ruleset does not duplicate them, which keeps
 * the endpoint safe to call again after a claim is amended.
 */
@Service
@Transactional
class FraudDetectionService(
	private val claimRepository: ClaimRepository,
	private val fraudAlertRepository: FraudAlertRepository,
	private val properties: FraudProperties,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val velocityWindow: Duration = Duration.ofDays(30)
	private val historyWindow: Duration = Duration.ofDays(365)

	fun evaluate(claim: Claim, save: Boolean = true): FraudEvaluation {
		val signals = mutableListOf<FraudSignal>()

		evaluateVelocity(claim)?.let(signals::add)
		evaluateAmount(claim)?.let(signals::add)
		evaluateHistory(claim)?.let(signals::add)
		evaluateIncidentConsistency(claim)?.let(signals::add)
		evaluateEvidence(claim)?.let(signals::add)

		val score = signals.sumOf { it.severity.weight }.coerceIn(0, 100)
		val flagged = score >= properties.autoAlertThreshold

		if (save) {
			claim.fraudScore = score
			claim.isFraudFlagged = flagged
			persistSignals(claim, signals)
			claimRepository.save(claim)
		}

		return FraudEvaluation(score = score, flagged = flagged, signals = signals)
	}

	private fun evaluateVelocity(claim: Claim): FraudSignal? {
		val customerId = claim.customer.id ?: return null
		val recent = claimRepository.countByCustomerSince(customerId, Instant.now().minus(velocityWindow))
		return when {
			recent >= 5 -> FraudSignal(
				ruleCode = "VELOCITY_MULTIPLE_30D",
				category = FraudCategory.VELOCITY,
				severity = FraudSeverity.HIGH,
				description = "$recent claims filed by this customer in the last 30 days",
			)
			recent >= 3 -> FraudSignal(
				ruleCode = "VELOCITY_ELEVATED_30D",
				category = FraudCategory.VELOCITY,
				severity = FraudSeverity.MEDIUM,
				description = "$recent claims filed by this customer in the last 30 days",
			)
			else -> null
		}
	}

	private fun evaluateAmount(claim: Claim): FraudSignal? {
		val estimated = claim.estimatedAmount ?: return null
		val sumInsured = claim.policy.sumInsured
		if (sumInsured > BigDecimal.ZERO && estimated > sumInsured) {
			return FraudSignal(
				ruleCode = "AMOUNT_EXCEEDS_SUM_INSURED",
				category = FraudCategory.AMOUNT,
				severity = FraudSeverity.HIGH,
				description = "Estimated amount $estimated exceeds the insured value $sumInsured",
			)
		}
		if (estimated > BigDecimal("2000000")) {
			return FraudSignal(
				ruleCode = "AMOUNT_ABOVE_THRESHOLD",
				category = FraudCategory.AMOUNT,
				severity = FraudSeverity.MEDIUM,
				description = "Estimated amount $estimated is above the LKR 2,000,000 review threshold",
			)
		}
		return null
	}

	private fun evaluateHistory(claim: Claim): FraudSignal? {
		val customerId = claim.customer.id ?: return null
		val rejected = claimRepository.countRejectedByCustomerSince(
			customerId,
			ClaimStatus.REJECTED,
			Instant.now().minus(historyWindow),
		)
		return when {
			rejected >= 3 -> FraudSignal(
				ruleCode = "HISTORY_MULTIPLE_REJECTED",
				category = FraudCategory.HISTORY,
				severity = FraudSeverity.HIGH,
				description = "$rejected claims from this customer were rejected in the last 12 months",
			)
			rejected == 2L -> FraudSignal(
				ruleCode = "HISTORY_PRIOR_REJECTIONS",
				category = FraudCategory.HISTORY,
				severity = FraudSeverity.LOW,
				description = "2 claims from this customer were rejected in the last 12 months",
			)
			else -> null
		}
	}

	/**
	 * Loss must predate the claim and sit inside the policy window. A claim dated
	 * after the incident, or against a policy that had not started, is inconsistent.
	 */
	private fun evaluateIncidentConsistency(claim: Claim): FraudSignal? {
		val incidentDate = claim.incidentDate
		if (incidentDate.isAfter(claim.submittedAt.plus(Duration.ofDays(1)))) {
			return FraudSignal(
				ruleCode = "TIMELINE_INCIDENT_AFTER_SUBMISSION",
				category = FraudCategory.BEHAVIOURAL,
				severity = FraudSeverity.CRITICAL,
				description = "Incident date is after the claim submission date",
			)
		}
		val incidentLocalDate = incidentDate.atZone(java.time.ZoneOffset.UTC).toLocalDate()
		if (!claim.policy.coversIncidentDate(incidentLocalDate)) {
			return FraudSignal(
				ruleCode = "POLICY_WINDOW_VIOLATION",
				category = FraudCategory.HISTORY,
				severity = FraudSeverity.CRITICAL,
				description = "Incident date falls outside the policy period " +
					"(${claim.policy.startDate} to ${claim.policy.endDate})",
			)
		}
		if (claim.policy.status == PolicyStatus.CANCELLED || claim.policy.status == PolicyStatus.LAPSED) {
			return FraudSignal(
				ruleCode = "POLICY_NOT_IN_FORCE",
				category = FraudCategory.HISTORY,
				severity = FraudSeverity.HIGH,
				description = "Policy ${claim.policy.policyNumber} was ${claim.policy.status.name.lowercase()}",
			)
		}
		return null
	}

	/**
	 * Theft and accident claims without a police report are the strongest
	 * documentary signal available before an adjuster looks at the file.
	 */
	private fun evaluateEvidence(claim: Claim): FraudSignal? {
		val hasPoliceReport = claim.documents.any { it.documentType == com.britam.insureclaim.claim.ClaimDocumentType.POLICE_REPORT }
		val needsReport = claim.incidentType.requiresPoliceReport ||
			claim.thirdPartyInvolved ||
			claim.incidentType == com.britam.insureclaim.claim.IncidentType.THEFT
		return if (needsReport && !hasPoliceReport && !claim.reportedByPolice) {
			FraudSignal(
				ruleCode = "DOCUMENTS_MISSING_POLICE_REPORT",
				category = FraudCategory.DOCUMENT,
				severity = FraudSeverity.MEDIUM,
				description = "No police report attached to a claim type that normally requires one",
			)
		} else {
			null
		}
	}

	/**
	 * Persists alerts for newly fired rules and drops alerts for rules that no
	 * longer fire, so the alert list always matches the current score.
	 */
	private fun persistSignals(claim: Claim, signals: List<FraudSignal>) {
		val claimId = claim.id ?: return
		val existing = fraudAlertRepository.findByClaimIdOrderByCreatedAtDesc(claimId).associateBy { it.ruleCode }
		val fired = signals.associateBy { it.ruleCode }

		(existing.keys - fired.keys).forEach { staleRule ->
			existing[staleRule]?.let { fraudAlertRepository.delete(it) }
		}

		signals.forEach { signal ->
			val alert = existing[signal.ruleCode] ?: FraudAlert(
				claim = claim,
				ruleCode = signal.ruleCode,
				category = signal.category,
				severity = signal.severity,
				scoreDelta = signal.severity.weight,
				description = signal.description,
			)
			// Keep the score contribution in step with the rule's current severity.
			alert.scoreDelta = signal.severity.weight
			alert.description = signal.description
			fraudAlertRepository.save(alert)
		}
	}

	// ------------------------------------------------------- alert triage ----

	@Transactional(readOnly = true)
	fun listAlerts(
		status: FraudAlertStatus?,
		severity: FraudSeverity?,
		page: Int,
		size: Int,
	): Page<FraudAlert> {
		val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by("createdAt").descending())
		return when {
			status != null -> fraudAlertRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
			severity != null -> fraudAlertRepository.findBySeverityOrderByCreatedAtDesc(severity, pageable)
			else -> fraudAlertRepository.findAll(pageable)
		}
	}

	fun updateAlertStatus(
		alertId: Long,
		status: FraudAlertStatus,
		resolution: String?,
		reviewerId: Long,
	): FraudAlert {
		val alert = fraudAlertRepository.findById(alertId)
			.orElseThrow { NotFoundException("Fraud alert", alertId) }
		if (alert.status.isSettled && status != alert.status) {
			throw BusinessRuleException(
				"Alert is already ${alert.status.name.lowercase()} and cannot be reopened",
				"ALERT_ALREADY_SETTLED",
			)
		}
		alert.status = status
		alert.reviewedBy = reviewerId
		alert.reviewedAt = Instant.now()
		alert.resolution = resolution?.take(500)
		return fraudAlertRepository.save(alert)
	}

	fun alertsForClaim(claimId: Long): List<FraudAlertSummary> =
		fraudAlertRepository.findByClaimIdOrderByCreatedAtDesc(claimId)
			.map {
				FraudAlertSummary(
					id = it.id ?: 0L,
					ruleCode = it.ruleCode,
					category = it.category.name,
					severity = it.severity.name,
					description = it.description,
					status = it.status.name,
					createdAt = it.createdAt,
				)
			}

	companion object {
		fun canReopen(status: FraudAlertStatus): Boolean = !status.isSettled

		fun summarise(alerts: List<FraudAlert>): Map<FraudSeverity, Long> =
			alerts.groupingBy { it.severity }.eachCount().mapValues { it.value.toLong() }
	}
}

data class FraudEvaluation(
	val score: Int,
	val flagged: Boolean,
	val signals: List<FraudSignal>,
)
