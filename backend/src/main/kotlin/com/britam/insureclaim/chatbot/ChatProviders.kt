package com.britam.insureclaim.chatbot

import io.swagger.v3.oas.annotations.media.Schema

/**
 * One AI provider the chatbot can talk to, with the models it exposes. Served
 * to the Settings screen so admins pick a provider and then a model from that
 * provider's list instead of typing raw values.
 */
@Schema(name = "ChatProvider", description = "An AI provider and the models it offers")
data class ChatProvider(
	val id: String,
	val label: String,
	val defaultModel: String,
	val models: List<String>,
	val keyPlaceholder: String,
	val keyHint: String,
	val docsUrl: String,
)

object ChatProviders {

	const val ANTHROPIC = "anthropic"
	const val OPENAI = "openai"
	const val GOOGLE = "google"
	const val CUSTOM = "custom"

	val all: List<ChatProvider> = listOf(
		ChatProvider(
			id = ANTHROPIC,
			label = "Anthropic (Claude)",
			defaultModel = "claude-sonnet-5",
			models = listOf(
				"claude-sonnet-5",
				"claude-opus-4-1",
				"claude-haiku-4-5",
				"claude-3-5-sonnet-latest",
				"claude-3-5-haiku-latest",
			),
			keyPlaceholder = "sk-ant-...",
			keyHint = "Create a key in the Anthropic console (starts with sk-ant-).",
			docsUrl = "https://console.anthropic.com/settings/keys",
		),
		ChatProvider(
			id = OPENAI,
			label = "OpenAI (GPT)",
			defaultModel = "gpt-4o",
			models = listOf(
				"gpt-4o",
				"gpt-4o-mini",
				"gpt-4.1",
				"gpt-4.1-mini",
				"o4-mini",
				"o3",
			),
			keyPlaceholder = "sk-...",
			keyHint = "Create a key in the OpenAI dashboard (starts with sk-).",
			docsUrl = "https://platform.openai.com/api-keys",
		),
		ChatProvider(
			id = GOOGLE,
			label = "Google (Gemini)",
			defaultModel = "gemini-2.0-flash",
			models = listOf(
				"gemini-2.0-flash",
				"gemini-2.5-flash",
				"gemini-2.5-pro",
				"gemini-1.5-pro",
			),
			keyPlaceholder = "AIza...",
			keyHint = "Create an API key in Google AI Studio (starts with AIza).",
			docsUrl = "https://aistudio.google.com/app/apikey",
		),
		ChatProvider(
			id = CUSTOM,
			label = "OpenAI-compatible (custom)",
			defaultModel = "gpt-4o-mini",
			models = listOf(
				"gpt-4o-mini",
				"gpt-4o",
				"llama-3.1-70b",
				"mistral-large",
			),
			keyPlaceholder = "your-api-key",
			keyHint = "Any OpenAI-compatible gateway. Point it with CHATBOT_BASE_URL.",
			docsUrl = "",
		),
	)

	val ids: List<String> = all.map { it.id }

	fun byId(id: String): ChatProvider? = all.firstOrNull { it.id == id }
}