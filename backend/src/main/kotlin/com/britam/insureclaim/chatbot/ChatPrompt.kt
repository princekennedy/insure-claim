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
			"When the user reports fraud, wishes to complain, or seems confused or frustrated, say clearly " +
			"that you have escalated the matter to the relevant Britam team and that someone will follow up. " +
			"Keep replies under 200 words and end with one question or next step."

	private const val COMPANY_KNOWLEDGE_HEADER =
		"\n\nCompany knowledge base (authoritative Britam information - prefer it over general knowledge):\n"

	/** Base prompt plus the configured company knowledge, when one is supplied. */
	fun system(companyInfo: String?): String {
		val knowledge = companyInfo?.trim().orEmpty()
		return if (knowledge.isEmpty()) BASE_SYSTEM_PROMPT else BASE_SYSTEM_PROMPT + COMPANY_KNOWLEDGE_HEADER + knowledge
	}
}
