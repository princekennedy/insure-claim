package com.britam.insureclaim.chatbot

/**
 * Builds the system prompt handed to the LLM. The base instructions keep the
 * assistant on-task; the admin-managed company knowledge base is appended so
 * operators can teach the bot about Britam products, processes and policies
 * without a redeploy.
 */
object ChatPrompt {

	private const val BASE_SYSTEM_PROMPT =
		"You are the Britam motor insurance claims assistant, a friendly and concise support bot. " +
			"Help customers with claim progress, payouts, policy cover, KYC verification, garage repairs " +
			"and portal issues. Never invent claim numbers, amounts or dates the user has not given you - " +
			"ask for a claim number and point them to the portal when you need specific data. " +
			"When a customer asks about a specific claim and no verified claim data is provided below, " +
			"ask for their claim number (format CLM-YYYY-XXXXXX) before answering. " +
			"When the user reports fraud, wishes to complain, or seems confused or frustrated, say clearly " +
			"that you have escalated the matter to the relevant Britam team and that someone will follow up. " +
			"Keep replies under 200 words and end with one question or next step."

	private const val COMPANY_KNOWLEDGE_HEADER =
		"\n\nCompany knowledge base (authoritative Britam information - prefer it over general knowledge):\n"

	private const val CLAIM_CONTEXT_HEADER =
		"\n\nVerified account data for this conversation (authoritative; do not contradict it):\n"

	/**
	 * Base prompt plus the enabled knowledge-base articles and, when the claim
	 * number has been verified, the customer's own claim data.
	 */
	fun system(knowledgeBase: String?, claimContext: String? = null): String {
		val knowledge = knowledgeBase?.trim().orEmpty()
		val claim = claimContext?.trim().orEmpty()
		return buildString {
			append(BASE_SYSTEM_PROMPT)
			if (knowledge.isNotEmpty()) append(COMPANY_KNOWLEDGE_HEADER).append(knowledge)
			if (claim.isNotEmpty()) append(CLAIM_CONTEXT_HEADER).append(claim)
		}
	}
}
