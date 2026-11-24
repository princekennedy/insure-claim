package com.britam.insureclaim.chatbot

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.claim.ClaimService
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.common.ValidationException
import com.britam.insureclaim.knowledge.KnowledgeService
import com.britam.insureclaim.settings.SettingService
import com.britam.insureclaim.user.User
import com.britam.insureclaim.user.UserRepository
import com.britam.insureclaim.security.ChatbotProperties
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.io.IOException
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class ChatService(
	private val conversationRepository: ChatConversationRepository,
	private val messageRepository: ChatMessageRepository,
	private val supportConcernRepository: SupportConcernRepository,
	private val userRepository: UserRepository,
	private val claimRepository: ClaimRepository,
	private val claimService: ClaimService,
	private val settingService: SettingService,
	private val knowledgeService: KnowledgeService,
	private val chatbotProperties: ChatbotProperties,
	private val objectMapper: ObjectMapper,
	restClientBuilder: RestClient.Builder,
) {

	private val log = LoggerFactory.getLogger(javaClass)

	private val restClient: RestClient = restClientBuilder
		.requestFactory(
			SimpleClientHttpRequestFactory().apply {
				setConnectTimeout(Duration.ofSeconds(10))
				setReadTimeout(Duration.ofSeconds(60))
			},
		)
		.build()

	fun config(): ChatConfigResponse {
		val live = settingService.chatbotHasProvider()
		return ChatConfigResponse(
			enabled = settingService.chatbotEnabled(),
			mode = if (live) MODE_LIVE else MODE_DEMO,
			model = if (live) settingService.chatbotModel() else null,
			greeting = GREETING,
		)
	}

	@Transactional(readOnly = true)
	fun myConversations(userId: Long?): List<ConversationResponse> {
		if (userId == null) return emptyList()
		return conversationRepository
			.findByUserIdAndStatusInOrderByStartedAtDesc(
				userId,
				listOf(ChatConversationStatus.OPEN, ChatConversationStatus.ESCALATED),
				org.springframework.data.domain.PageRequest.of(0, 10),
			).content.map { ConversationResponse.from(it) }
	}

	fun createConversation(userId: Long?, request: CreateConversationRequest): ConversationResponse {
		if (!settingService.chatbotEnabled()) {
			throw BusinessRuleException(
				"The assistant is currently disabled. Please try again later.",
				"CHATBOT_DISABLED",
			)
		}
		val user = userId?.let { userRepository.findById(it).orElse(null) }
		val claim = request.claimId?.let { claimId ->
			if (user == null) {
				throw ForbiddenException("Sign in to discuss a claim with the assistant")
			}
			val found = claimRepository.findById(claimId).orElseThrow { NotFoundException("Claim", claimId) }
			if (!user.isStaff() && found.customer.id != user.id) {
				throw ForbiddenException("This claim belongs to another customer")
			}
			found
		}
		val conversation = conversationRepository.save(
			ChatConversation(
				user = user,
				claim = claim,
				reference = "IC-CH-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase(),
				status = ChatConversationStatus.OPEN,
			),
		)
		return ConversationResponse.from(conversation)
	}

	@Transactional(readOnly = true)
	fun conversationDetail(conversationId: Long, userId: Long?): ConversationDetailResponse {
		val conversation = requireConversation(conversationId, userId)
		val messages = messageRepository.findByConversationIdOrderByCreatedAt(conversationId)
			.map { MessageResponse.from(it) }
		return ConversationDetailResponse(ConversationResponse.from(conversation), messages)
	}

	fun sendMessage(conversationId: Long, userId: Long?, request: SendMessageRequest): ChatSendResponse {
		if (!settingService.chatbotEnabled()) {
			throw BusinessRuleException(
				"The assistant is currently disabled. Please try again later.",
				"CHATBOT_DISABLED",
			)
		}
		val conversation = requireConversation(conversationId, userId)
		if (conversation.status == ChatConversationStatus.CLOSED) {
			throw BusinessRuleException("This conversation is closed", "CONVERSATION_CLOSED")
		}

		val content = request.content.trim()
		val now = Instant.now()
		val userMessage = messageRepository.save(
			ChatMessage(
				conversation = conversation,
				sender = ChatSender.CUSTOMER,
				content = content,
				createdAt = now,
			),
		)

		val classification = ChatIntents.classify(content)
		val lookup = resolveClaimLookup(conversation, content)
		val shouldEscalate = shouldEscalate(content, classification, lookup)
		val reply = generateReply(conversation, classification, lookup)

		val assistantMessage = messageRepository.save(
			ChatMessage(
				conversation = conversation,
				sender = ChatSender.ASSISTANT,
				content = reply,
				intent = classification.intent,
				confidence = classification.confidence.toBigDecimal(),
				escalateSuggested = shouldEscalate,
				createdAt = now.plusMillis(1),
			),
		)

		val escalated = shouldEscalate
		var concern: SupportConcernResponse? = null
		if (shouldEscalate) {
			conversation.escalate(now)
			concern = SupportConcernResponse.from(
				supportConcernRepository.save(
					SupportConcern(
						conversation = conversation,
						user = conversation.user,
						claim = conversation.claim,
						category = classification.category ?: ConcernCategory.OTHER,
						severity = if (classification.category?.needsHuman == true) ConcernSeverity.HIGH else ConcernSeverity.MEDIUM,
						summary = content.take(MAX_CONCERN_SUMMARY),
					),
				),
			)
		}
		conversation.messagesCount += 2

		return ChatSendResponse(
			conversation = ConversationResponse.from(conversationRepository.save(conversation)),
			userMessage = MessageResponse.from(userMessage),
			assistantMessage = MessageResponse.from(assistantMessage),
			escalated = escalated,
			concern = concern,
		)
	}

	@Transactional(readOnly = true)
	fun staffConcerns(): List<SupportConcernResponse> =
		supportConcernRepository
			.findAll(org.springframework.data.domain.PageRequest.of(0, 50))
			.content.map { SupportConcernResponse.from(it) }

	/**
	 * Diagnostic used by the Settings screen: sends a tiny prompt to the chosen
	 * provider and reports whether the credentials/model actually work. Values
	 * passed in the request take precedence over stored settings so admins can
	 * verify a key before saving it. Provider failures are returned as
	 * `ok = false` rather than thrown, so the UI can show the reason.
	 */
	fun testProvider(request: ChatProviderTestRequest): ChatProviderTestResponse {
		val provider = request.provider?.takeIf { it.isNotBlank() } ?: settingService.chatbotProvider()
		if (!ChatProviders.ids.contains(provider)) {
			throw ValidationException("Unknown AI provider: $provider", "UNKNOWN_CHATBOT_PROVIDER")
		}
		val model = request.model?.takeIf { it.isNotBlank() }
			?: ChatProviders.byId(provider)?.defaultModel
			?: settingService.chatbotModel()
		val apiKey = request.apiKey?.takeIf { it.isNotBlank() } ?: settingService.chatbotApiKey()
		if (apiKey.isBlank()) {
			return ChatProviderTestResponse(
				ok = false,
				provider = provider,
				model = model,
				latencyMs = 0,
				message = "Add an API key before testing the connection.",
			)
		}

		val systemPrompt = ChatPrompt.system(knowledgeService.promptText())
		val history = listOf(ChatSender.CUSTOMER to "Reply with the single word: OK")
		val startedAt = System.nanoTime()
		return try {
			val baseUrl = baseUrlFor(provider)
			val reply = when (provider) {
				ChatProviders.OPENAI, ChatProviders.CUSTOM ->
					openAiReply(history, model, TEST_MAX_TOKENS, apiKey, baseUrl, systemPrompt)
				ChatProviders.GOOGLE ->
					geminiReply(history, model, TEST_MAX_TOKENS, apiKey, baseUrl, systemPrompt)
				else ->
					anthropicReply(history, model, TEST_MAX_TOKENS, apiKey, baseUrl, systemPrompt)
			}
			ChatProviderTestResponse(
				ok = true,
				provider = provider,
				model = model,
				latencyMs = elapsedMs(startedAt),
				message = "Connected successfully.",
				reply = reply?.take(MAX_TEST_REPLY),
			)
		} catch (e: Exception) {
			log.warn("Chat provider test failed for $provider/$model", e)
			ChatProviderTestResponse(
				ok = false,
				provider = provider,
				model = model,
				latencyMs = elapsedMs(startedAt),
				message = e.message ?: "The provider could not be reached.",
			)
		}
	}

	private fun elapsedMs(startedAt: Long): Long = (System.nanoTime() - startedAt) / 1_000_000

	// --------------------------------------------------------- internals -----

	private data class ClaimLookup(
		val context: ClaimChatContext? = null,
		val promptNote: String? = null,
		val fallbackReply: String? = null,
	)

	private fun requireConversation(conversationId: Long, userId: Long?): ChatConversation {
		val conversation = conversationRepository.findById(conversationId)
			.orElseThrow { NotFoundException("Conversation", conversationId) }
		if (userId != null) {
			val user = userRepository.findById(userId).orElse(null)
			if (user != null && user.isStaff()) return conversation
			val ownerId = conversation.user?.id
			if (ownerId != null && ownerId != userId) {
				throw ForbiddenException("This conversation belongs to another user")
			}
		}
		return conversation
	}

	private fun shouldEscalate(
		content: String,
		classification: ChatClassification,
		lookup: ClaimLookup,
	): Boolean = when {
		classification.category == null -> false
		// A bare claim number (or a claim we could resolve) is a lookup, not a
		// complaint: answer it instead of opening a support concern.
		classification.category == ConcernCategory.OTHER &&
			(lookup.context != null || ChatClaimContextBuilder.extractClaimNumber(content) != null) -> false
		classification.category.needsHuman -> true
		classification.category == ConcernCategory.OTHER -> true
		classification.confidence < settingService.chatbotEscalateThreshold() -> true
		else -> false
	}

	/**
	 * Resolves the claim/customer data the assistant may use for this message.
	 * Claim numbers are only honoured for the signed-in owner (staff may look up
	 * any claim); anonymous visitors are never shown customer data.
	 */
	private fun resolveClaimLookup(conversation: ChatConversation, content: String): ClaimLookup {
		val owner = conversation.user
		conversation.claim?.let { existing ->
			return if (owner == null) {
				ClaimLookup(
					promptNote = "This conversation is anonymous, so claim data must not be disclosed. " +
						"Ask the customer to sign in to view their claim.",
					fallbackReply = ChatClaimContextBuilder.anonymousReply(),
				)
			} else {
				contextFor(existing, owner)
			}
		}

		val claimNumber = ChatClaimContextBuilder.extractClaimNumber(content) ?: return ClaimLookup()

		if (owner == null) {
			return ClaimLookup(
				promptNote = "The customer supplied claim number $claimNumber but is not signed in. " +
					"Do not look up or reveal any claim or customer data; ask them to sign in or use their tracking link.",
				fallbackReply = ChatClaimContextBuilder.anonymousReply(),
			)
		}

		val claim = claimRepository.findByClaimNumberWithDetails(claimNumber).orElse(null)
		if (claim == null || (!owner.isStaff() && claim.customer.id != owner.id)) {
			return ClaimLookup(
				promptNote = "The customer supplied claim number $claimNumber but it is not on their account. " +
					"Do not invent details; ask them to check the number or sign in with the right account.",
				fallbackReply = ChatClaimContextBuilder.notFoundReply(claimNumber),
			)
		}

		if (conversation.claim == null) conversation.claim = claim
		return contextFor(claim, owner)
	}

	private fun contextFor(claim: Claim, actor: User): ClaimLookup = try {
		ClaimLookup(context = ChatClaimContextBuilder.build(claim, claimService.toDetail(claim, actor)))
	} catch (e: Exception) {
		log.warn("Could not build claim context for {}", claim.claimNumber, e)
		ClaimLookup()
	}

	private fun generateReply(
		conversation: ChatConversation,
		classification: ChatClassification,
		lookup: ClaimLookup,
	): String {
		val fallback = lookup.fallbackReply ?: ChatIntents.replyFor(classification, GREETING)
		if (!settingService.chatbotHasProvider()) {
			return fallback
		}
		return try {
			llmReply(conversation, lookup) ?: fallback
		} catch (e: Exception) {
			log.warn("LLM reply failed for conversation ${conversation.id}, falling back to rule-based bot", e)
			fallback
		}
	}

	/**
	 * Calls the configured provider (Anthropic, OpenAI, Google Gemini or an
	 * OpenAI-compatible gateway) with the conversation history. Returns the
	 * assistant text or null when the provider returned nothing usable.
	 * Throws on HTTP errors / network failures so callers can fall back.
	 */
	private fun llmReply(conversation: ChatConversation, lookup: ClaimLookup): String? {
		val provider = settingService.chatbotProvider()
		val apiKey = settingService.chatbotApiKey()
		val model = settingService.chatbotModel()
		val maxTokens = settingService.chatbotMaxTokens()
		val history = messageRepository.findByConversationIdOrderByCreatedAt(conversation.id ?: 0L)
			.takeLast(MAX_LLM_HISTORY)
			.filter { it.sender != ChatSender.AGENT }
			.map { it.sender to it.content }
		val systemPrompt = ChatPrompt.system(
			knowledgeService.promptText(),
			lookup.context?.promptBlock ?: lookup.promptNote,
		)

		val baseUrl = baseUrlFor(provider)
		return when (provider) {
			ChatProviders.OPENAI, ChatProviders.CUSTOM ->
				openAiReply(history, model, maxTokens, apiKey, baseUrl, systemPrompt)
			ChatProviders.GOOGLE ->
				geminiReply(history, model, maxTokens, apiKey, baseUrl, systemPrompt)
			else ->
				anthropicReply(history, model, maxTokens, apiKey, baseUrl, systemPrompt)
		}
	}

	/** Base URL per provider; anthropic/custom honour the environment override. */
	private fun baseUrlFor(provider: String): String = when (provider) {
		ChatProviders.OPENAI -> OPENAI_BASE_URL
		ChatProviders.GOOGLE -> GOOGLE_BASE_URL
		else -> chatbotProperties.baseUrl.trimEnd('/')
	}

	private fun anthropicReply(
		history: List<Pair<ChatSender, String>>,
		model: String,
		maxTokens: Int,
		apiKey: String,
		baseUrl: String,
		systemPrompt: String,
	): String? {
		val body = objectMapper.createObjectNode().apply {
			put("model", model)
			put("max_tokens", maxTokens)
			put("system", systemPrompt)
			set(
				"messages",
				objectMapper.createArrayNode().apply {
					history.forEach { (sender, text) ->
						addObject()
							.put("role", if (sender == ChatSender.CUSTOMER) "user" else "assistant")
							.put("content", text)
					}
				},
			)
		}

		val root = restClient.post()
			.uri("$baseUrl/v1/messages")
			.header("x-api-key", apiKey)
			.header("anthropic-version", "2023-06-01")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.exchange { _, response ->
				if (response.statusCode.isError) {
					throw IOException("Anthropic returned HTTP ${response.statusCode.value()}")
				}
				objectMapper.readTree(response.body)
			}
			?: throw IOException("Anthropic returned an empty response")

		return (root["content"] as? ArrayNode)?.elements()?.asSequence()
			?.firstOrNull { it.path("type").asString() == "text" }
			?.path("text")
			?.takeIf { it.isString }
			?.asString()
			?.trim()
			?.takeUnless { it.isBlank() }
			?: throw IOException("Anthropic returned no text content")
	}

	private fun openAiReply(
		history: List<Pair<ChatSender, String>>,
		model: String,
		maxTokens: Int,
		apiKey: String,
		baseUrl: String,
		systemPrompt: String,
	): String? {
		val body = objectMapper.createObjectNode().apply {
			put("model", model)
			put("max_tokens", maxTokens)
			set(
				"messages",
				objectMapper.createArrayNode().apply {
					addObject().put("role", "system").put("content", systemPrompt)
					history.forEach { (sender, text) ->
						addObject()
							.put("role", if (sender == ChatSender.CUSTOMER) "user" else "assistant")
							.put("content", text)
					}
				},
			)
		}

		val root = restClient.post()
			.uri("$baseUrl/v1/chat/completions")
			.header("Authorization", "Bearer $apiKey")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.exchange { _, response ->
				if (response.statusCode.isError) {
					throw IOException("OpenAI returned HTTP ${response.statusCode.value()}")
				}
				objectMapper.readTree(response.body)
			}
			?: throw IOException("OpenAI returned an empty response")

		return root.path("choices").path(0).path("message").path("content")
			.takeIf { it.isString }
			?.asString()
			?.trim()
			?.takeUnless { it.isBlank() }
			?: throw IOException("OpenAI returned no text content")
	}

	private fun geminiReply(
		history: List<Pair<ChatSender, String>>,
		model: String,
		maxTokens: Int,
		apiKey: String,
		baseUrl: String,
		systemPrompt: String,
	): String? {
		val body = objectMapper.createObjectNode().apply {
			set(
				"system_instruction",
				objectMapper.createObjectNode().apply {
					set(
						"parts",
						objectMapper.createArrayNode().apply { addObject().put("text", systemPrompt) },
					)
				},
			)
			set(
				"contents",
				objectMapper.createArrayNode().apply {
					history.forEach { (sender, text) ->
						addObject()
							.put("role", if (sender == ChatSender.CUSTOMER) "user" else "model")
							.set(
								"parts",
								objectMapper.createArrayNode().apply { addObject().put("text", text) },
							)
					}
				},
			)
			set("generationConfig", objectMapper.createObjectNode().put("maxOutputTokens", maxTokens))
		}

		val root = restClient.post()
			.uri("$baseUrl/v1beta/models/$model:generateContent?key=$apiKey")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.exchange { _, response ->
				if (response.statusCode.isError) {
					throw IOException("Google Gemini returned HTTP ${response.statusCode.value()}")
				}
				objectMapper.readTree(response.body)
			}
			?: throw IOException("Google returned an empty response")

		val parts = root.path("candidates").path(0).path("content").path("parts") as? ArrayNode
			?: throw IOException("Google returned no content")
		return parts.elements().asSequence()
			.mapNotNull { it.path("text").takeIf { text -> text.isString }?.asString() }
			.joinToString("")
			.trim()
			.takeUnless { it.isBlank() }
			?: throw IOException("Google returned no text content")
	}

	companion object {
		private const val MODE_LIVE = "live"
		private const val MODE_DEMO = "demo"
		private const val MAX_CONCERN_SUMMARY = 500
		private const val MAX_LLM_HISTORY = 12
		private const val TEST_MAX_TOKENS = 48
		private const val MAX_TEST_REPLY = 200

		private const val OPENAI_BASE_URL = "https://api.openai.com"
		private const val GOOGLE_BASE_URL = "https://generativelanguage.googleapis.com"

		private const val GREETING =
			"Hello! I'm the Britam claims assistant. I can help with claim updates, payouts, policy cover, " +
				"KYC verification and garage repairs. Send your claim number (for example CLM-2026-AB12CD) " +
				"and I'll look up the latest status for you."
	}
}