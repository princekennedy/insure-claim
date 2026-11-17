package com.britam.insureclaim.chatbot

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimDetailResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Claim and customer facts the assistant may use after the claim number has been
 * verified against the signed-in account. [promptBlock] gives the LLM precise,
 * customer-safe data; [fallbackReply] keeps the bot useful without a provider.
 */
data class ClaimChatContext(
	val claimNumber: String,
	val promptBlock: String,
	val fallbackReply: String,
)

object ChatClaimContextBuilder {

	/** Generated claim numbers look like CLM-2026-AB12CD. */
	private val CLAIM_NUMBER_PATTERN =
		Regex("""\bCLM[- ]?(\d{4})[- ]?([A-Z0-9]{4,8})\b""", RegexOption.IGNORE_CASE)

	private val INSTANT_DATE: DateTimeFormatter =
		DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK).withZone(ZoneOffset.UTC)

	private val LOCAL_DATE: DateTimeFormatter =
		DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

	/** Normalises a claim number typed in any common spacing/case back to CLM-YYYY-CODE. */
	fun extractClaimNumber(message: String): String? {
		val match = CLAIM_NUMBER_PATTERN.find(message) ?: return null
		return "CLM-${match.groupValues[1]}-${match.groupValues[2].uppercase()}"
	}

	fun notFoundReply(claimNumber: String): String =
		"I couldn't find claim $claimNumber on your account. Please check the number " +
			"(it looks like CLM-2026-AB12CD) and send it again. If you're not signed in, sign in " +
			"first so I can pull up your claim securely."

	fun anonymousReply(): String =
		"I can only look up claim and customer details for a signed-in account. Please sign in to " +
			"your Britam account and send the claim number again, or use the tracking link from your " +
			"claim email."

	fun build(claim: Claim, detail: ClaimDetailResponse): ClaimChatContext {
		val summary = detail.summary
		val policy = claim.policy
		val location = detail.incidentLocation?.takeIf { it.isNotBlank() }
		val vehicleColor = detail.vehicle.color?.takeIf { it.isNotBlank() }

		val facts = buildList {
			add("Claim number: ${summary.claimNumber}")
			add("Account holder: ${detail.customer.fullName}")
			add("Claim status: ${summary.statusLabel} (${summary.progressPercent}% through the claims journey)")
			add(
				"Incident: ${summary.incidentType} on ${INSTANT_DATE.format(summary.incidentDate)}" +
					(location?.let { " at $it" } ?: ""),
			)
			if (detail.description.isNotBlank()) {
				add("Reported description: ${detail.description.take(800)}")
			}
			add(
				"Amounts: estimated ${money(summary.estimatedAmount)}; approved ${money(summary.approvedAmount)}; " +
					"policy excess ${money(summary.excessAmount)}; net settlement ${money(summary.netSettlement)}",
			)
			add(
				"Policy: ${policy.policyNumber} (${policy.productCode}) with ${policy.insurerName}, " +
					"valid ${LOCAL_DATE.format(policy.startDate)} to ${LOCAL_DATE.format(policy.endDate)}, " +
					"status ${policy.status}",
			)
			add(
				"Policy amounts: premium ${money(policy.premiumAmount)}; sum insured ${money(policy.sumInsured)}; " +
					"excess ${money(policy.excessAmount)}",
			)
			add(
				"Vehicle: ${detail.vehicle.registrationNumber} - ${detail.vehicle.make} " +
					"${detail.vehicle.model} (${detail.vehicle.year})" +
					(vehicleColor?.let { ", $it" } ?: ""),
			)
			detail.kycStatus?.takeIf { it.isNotBlank() }?.let { add("KYC verification: $it") }
			detail.repair?.let { repair ->
				add(
					"Repair: ${repair.statusLabel} at ${repair.garageName} (${repair.garagePhone}); " +
						"${repair.progressPercent}% complete, estimated " +
						"${money(repair.finalAmount ?: repair.quotedAmount)}" +
						(repair.estimatedDays?.let { ", $it days expected" } ?: "") +
						(repair.notes?.takeIf { it.isNotBlank() }?.let { "; note: ${it.take(300)}" } ?: ""),
				)
			}
			if (detail.documents.isNotEmpty()) {
				add(
					"Documents on file: " +
						detail.documents.joinToString { it.documentType.name.lowercase().replace('_', ' ') },
				)
			}
			val timeline = detail.timeline.takeLast(6)
			if (timeline.isNotEmpty()) {
				add(
					"Recent timeline: " +
						timeline.joinToString(" | ") { event ->
							"${INSTANT_DATE.format(event.occurredAt)} ${event.label}" +
								(event.note?.takeIf { it.isNotBlank() }?.let { ": ${it.take(160)}" } ?: "")
						},
				)
			}
			if (detail.nextStages.isNotEmpty()) {
				add("Next available stages: " + detail.nextStages.joinToString { it.label })
			}
		}

		val promptBlock = buildString {
			appendLine("VERIFIED CLAIM DATA for the signed-in account holder. Use only these facts when")
			appendLine("answering questions about this claim. Do not invent amounts, dates or statuses.")
			facts.forEach { appendLine("- $it") }
		}

		val replyLines = buildList {
			add("Thanks ${detail.customer.fullName.ifBlank { "there" }}, here is what I can see for claim ${summary.claimNumber}:")
			add("• Status: ${summary.statusLabel} (${summary.progressPercent}% complete)")
			add(
				"• Incident: ${pretty(summary.incidentType.name)} on ${INSTANT_DATE.format(summary.incidentDate)}" +
					(location?.let { " at $it" } ?: ""),
			)
			add(
				"• Vehicle: ${detail.vehicle.registrationNumber} - ${detail.vehicle.make} " +
					"${detail.vehicle.model} (${detail.vehicle.year})",
			)
			add(
				"• Amounts: approved ${money(summary.approvedAmount)}; net settlement " +
					"${money(summary.netSettlement)} (policy excess ${money(summary.excessAmount)})",
			)
			detail.repair?.let { repair ->
				add(
					"• Repair: ${repair.statusLabel} at ${repair.garageName}, ${repair.progressPercent}% complete",
				)
			}
			add(
				"• Next step: " +
					(detail.nextStages.firstOrNull()?.label ?: "no customer action is needed right now"),
			)
			add("Is there anything else you'd like me to check on this claim?")
		}

		return ClaimChatContext(
			claimNumber = summary.claimNumber,
			promptBlock = promptBlock,
			fallbackReply = replyLines.joinToString("\n"),
		)
	}

	private fun money(value: BigDecimal?): String =
		value?.let {
			val format = NumberFormat.getNumberInstance(Locale.US).apply {
				minimumFractionDigits = 2
				maximumFractionDigits = 2
			}
			"KES " + format.format(it.setScale(2, RoundingMode.HALF_UP))
		} ?: "not set"

	private fun pretty(enumName: String): String =
		enumName.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase(Locale.UK) }
}
