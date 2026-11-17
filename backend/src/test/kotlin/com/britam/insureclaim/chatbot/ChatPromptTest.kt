package com.britam.insureclaim.chatbot

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChatPromptTest {

	@Test
	fun `falls back to the base prompt when no company knowledge is set`() {
		val prompt = ChatPrompt.system("   ")

		assertFalse(prompt.contains("Company knowledge base"))
		assertContains(prompt, "Britam motor insurance claims assistant")
	}

	@Test
	fun `appends the company knowledge to the system prompt`() {
		val knowledge = "Britam Comprehensive cover includes windscreen and third-party fire and theft."
		val prompt = ChatPrompt.system(knowledge)

		assertContains(prompt, "Company knowledge base")
		assertContains(prompt, knowledge)
	}

	@Test
	fun `keeps the base instructions before the knowledge block`() {
		val prompt = ChatPrompt.system("Britam claims hotline is 0709 100 000.")

		assertEquals(
			true,
			prompt.indexOf("Britam motor insurance claims assistant") <
				prompt.indexOf("Company knowledge base"),
		)
	}
}
