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
			defaultModel = "claude-sonnet-5-5",
			models = listOf(
				"claude-sonnet-5-5",
				"claude-haiku-5-5",
				"claude-opus-5-5",
				"claude-fable-5-1",
				"claude-sonnet-5",
			),
			keyPlaceholder = "sk-ant-...",
			keyHint = "Create a key in the Anthropic console (starts with sk-ant-).",
			docsUrl = "https://console.anthropic.com/settings/keys",
		),
		ChatProvider(
			id = OPENAI,
			label = "OpenAI (GPT)",
			defaultModel = "gpt-6.1-sol",
			models = listOf(
				"gpt-6.1-sol",
				"gpt-6-astra",
				"gpt-6-luna",
				"gpt-5.6-sol",
				"gpt-5.6-terra",
				"gpt-5.6-luna",
			),
			keyPlaceholder = "sk-...",
			keyHint = "Create a key in the OpenAI dashboard (starts with sk-).",
			docsUrl = "https://platform.openai.com/api-keys",
		),
		ChatProvider(
			id = GOOGLE,
			label = "Google (Gemini)",
			defaultModel = "gemini-3.8-flash",
			models = listOf(
				"gemini-3.8-flash",
				"gemini-3.7-flash",
				"gemini-3.6-flash",
				"gemini-3.5-flash",
				"gemini-3.5-flash-lite",
				"gemini-3.1-pro-preview",
			),
			keyPlaceholder = "AIza...",
			keyHint = "Create an API key in Google AI Studio (starts with AIza).",
			docsUrl = "https://aistudio.google.com/app/apikey",
		),
		ChatProvider(
			id = CUSTOM,
			label = "OpenAI-compatible (custom)",
			defaultModel = "gpt-6-luna",
			models = listOf(
				"gpt-6-luna",
				"gpt-5.6-luna",
				"gpt-oss-120b",
				"gpt-oss-20b",
				"deepseek-v4-flash",
			),
			keyPlaceholder = "your-api-key",
			keyHint = "Any OpenAI-compatible gateway. Point it with CHATBOT_BASE_URL.",
			docsUrl = "",
		),
	)

	val ids: List<String> = all.map { it.id }

	fun byId(id: String): ChatProvider? = all.firstOrNull { it.id == id }
}