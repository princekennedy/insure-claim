package com.britam.insureclaim.chatbot

/**
 * Result of classifying a customer message without an LLM.
 *
 * [category] is null for a greeting (nothing to escalate). The intent stays a
 * plain label here because the same value is stored on the assistant message
 * for the audit trail.
 */
data class ChatClassification(
	val intent: String?,
	val category: ConcernCategory?,
	val confidence: Double,
	val keywordHits: Int,
)

/**
 * Keyword-driven intent classification and canned replies used when no LLM key
 * is configured (the demo/"rule-based" bot) or as a fallback when the provider
 * call fails. Categories mirror [ConcernCategory] so escalation works end to end.
 */
object ChatIntents {

	private data class Rule(val category: ConcernCategory, val intent: String, val keywords: List<String>)

	private val greetingPattern = Regex(
		"""^\s*(hi|hello|hey|howdy|good (morning|afternoon|evening)|morning|afternoon|evening)\s*[!\.,]?\s*$""",
		RegexOption.IGNORE_CASE,
	)

	private val rules: List<Rule> = listOf(
		Rule(ConcernCategory.CLAIM_DELAY, "CLAIM_STATUS", listOf(
			"claim status", "claim update", "my claim", "claim number", "track claim", "follow up",
			"how long", "stuck", "pending", "delayed", "delay", "delays", "process", "taking long", "processing", "waiting",
		)),
		Rule(ConcernCategory.PAYOUT, "PAYOUT", listOf(
			"payout", "payouts", "pay out", "settlement", "settle", "paid", "payment", "money",
			"reimburse", "reimbursement", "refund", "compensation", "bank", "cheque", "mpesa", "m-pesa",
		)),
		Rule(ConcernCategory.KYC, "KYC", listOf(
			"kyc", "verification", "verify", "verif", "documents", "document", "id card", "national id",
			"passport", "identity", "upload id", "approve id",
		)),
		Rule(ConcernCategory.GARAGE_QUALITY, "GARAGE", listOf(
			"garage", "workshop", "repair", "mechanic", "estimate", "panel beater", "assessed",
			"quality of repair", "repair quality",
		)),
		Rule(ConcernCategory.POLICY, "POLICY", listOf(
			"policy", "cover", "coverage", "premium", "premiums", "renew", "renewal", "excess",
			"deductible", "comprehensive", "third party",
		)),
		Rule(ConcernCategory.FRAUD_FLAG, "FRAUD", listOf(
			"fraud", "stolen", "fake", "scam", "police", "suspicious", "false claim", "report fraud",
		)),
		Rule(ConcernCategory.COMPLAINT, "COMPLAINT", listOf(
			"complain", "complaint", "rude", "terrible", "worst", "disappointed", "unhappy", "angry", "frustrated",
		)),
		Rule(ConcernCategory.TECHNICAL, "TECHNICAL", listOf(
			"website", "portal", "login", "sign in", "password", "error", "bug", "upload", "payment failed",
			"page not loading", "technical",
		)),
	)

	/** Lowercased, punctuation stripped, single spaces. */
	fun normalize(text: String): String =
		text.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim().replace(Regex("\\s+"), " ")

	fun classify(rawText: String): ChatClassification {
		val text = normalize(rawText)
		if (text.isBlank()) {
			return ChatClassification(null, null, 0.0, 0)
		}
		if (greetingPattern.matches(text)) {
			return ChatClassification("GREETING", null, 0.97, 1)
		}

		val words = text.split(" ")
		val wordSet = words.toSet()
		var best: Rule? = null
		var bestHits = 0
		for (rule in rules) {
			var hits = 0
			for (keyword in rule.keywords) {
				if (keyword.contains(' ')) {
					if (text.contains(keyword)) hits++
				} else if (keyword in wordSet) {
					hits++
				}
			}
			if (hits > bestHits) {
				best = rule
				bestHits = hits
			}
		}

		if (best == null || bestHits == 0) {
			return ChatClassification(null, ConcernCategory.OTHER, 0.2, 0)
		}
		val confidence = minOf(0.95, 0.55 + 0.15 * bestHits)
		return ChatClassification(
			intent = best.intent,
			category = best.category,
			confidence = confidence,
			keywordHits = bestHits,
		)
	}

	/**
	 * Canned, brand-consistent replies used by the demo bot. Greetings reuse the
	 * configured greeting from the settings screen.
	 */
	fun replyFor(classification: ChatClassification, greeting: String): String {
		val category = classification.category ?: return greeting
		return when (category) {
			ConcernCategory.CLAIM_DELAY ->
				"You can follow every step of your claim under *Claims* in your dashboard - it shows the current " +
					"stage and what happens next. Most claims move to settlement as soon as the assessment and payout " +
					"documents are complete. If yours has been waiting longer than expected, share your claim number " +
					"and I'll flag it for follow-up."

			ConcernCategory.PAYOUT ->
				"Once a claim is approved, payouts go to your registered bank or mobile money account after the " +
					"settlement documents are signed. If you've been waiting beyond the promised window, tell me your " +
					"claim number and I'll chase it for you."

			ConcernCategory.KYC ->
				"Accounts usually need a one-time KYC confirmation before the first payout. Open the *Verification* " +
					"section in your profile and upload your ID or passport - it clears within a few hours."

			ConcernCategory.GARAGE_QUALITY ->
				"Your claim shows the Britam-accredited garage assigned to the repair, including its rating. If the " +
					"estimate, quality of repairs or turnaround seems off, I can raise it with our assessments team."

			ConcernCategory.POLICY ->
				"Your policy cover, excess and renewal dates are listed under *Policies* in your dashboard. Tell me " +
					"what you'd like to confirm - comprehensive cover, excess or renewals - and I'll point you to the " +
					"right place."

			ConcernCategory.FRAUD_FLAG ->
				"I'm sorry to hear about this - that's very serious. I've flagged it to our fraud team for immediate " +
					"review and they'll contact you shortly. In the meantime, please keep any police report or " +
					"evidence safe."

			ConcernCategory.COMPLAINT ->
				"I'm sorry to hear that, and thank you for telling us. I've logged this as a complaint with our " +
					"customer care team, and a manager will reach out to you within 24 hours."

			ConcernCategory.TECHNICAL ->
				"Most portal issues clear by signing out and back in. If it keeps happening, tell me which page " +
					"and what you were doing, and I'll log it for our support team."

			ConcernCategory.OTHER ->
				"I want to make sure you get the right help. Could you rephrase your question? I can help with claim " +
					"status, payouts, policy cover, KYC verification and garage repairs - or I can pass your note to " +
					"our team for a human to follow up."
		}
	}
}