package com.britam.insureclaim.chatbot

import com.britam.insureclaim.claim.ClaimRepository
import com.britam.insureclaim.common.BusinessRuleException
import com.britam.insureclaim.common.ForbiddenException
import com.britam.insureclaim.common.NotFoundException
import com.britam.insureclaim.settings.SettingService
import com.britam.insureclaim.user.UserRepository
import com.britam.insureclaim.security.ChatbotProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
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
	private val settingService: SettingService,
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

	fun config(): ChatConfigResponse = ChatConfigResponse(
		enabled = settingService.chatbotEnabled(),
		mode = if (chatbotProperties.hasLlmProvider) MODE_LIVE else MODE_DEMO,
		model = if (chatbotProperties.hasLlmProvider) settingService.chatbotModel() else null,
		greeting = GREETING,
	)

	@Transactional(readOnly = true)
	fun myConversations(userId: Long?): List<ConversationResponse> {
		if (userId == null) return emptyList()
		return conversationRepository
			.findByUserIdAndStatusInOrderByStartedAtDesc(
				userId,
				listOf(ChatConversationStatus.OPEN, ChatConversationStatus.ESCALATED),
				org.springframework.data.domain.PageRequest.of(0, 10),
			).map { ConversationResponse.from(it) }
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
			val found = claimRepository.findById(claimId).orElseThrow { NotFoundException("Claim", claimId) }
			if (user != null && !user.isStaff() && found.customer.id != user.id) {
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
		val shouldEscalate = shouldEscalate(classification)
		val reply = generateReply(conversation, content, classification)

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
			.map { SupportConcernResponse.from(it) }

	// --------------------------------------------------------- internals -----

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

	private fun shouldEscalate(classification: ChatClassification): Boolean = when {
		classification.category == null -> false
		classification.category.needsHuman -> true
		classification.category == ConcernCategory.OTHER -> true
		classification.confidence < settingService.chatbotEscalateThreshold() -> true
		else -> false
	}

	private fun generateReply(
		conversation: ChatConversation,
		content: String,
		classification: ChatClassification,
	): String {
		if (!chatbotProperties.hasLlmProvider) {
			return ChatIntents.replyFor(classification, GREETING)
		}
		return try {
			llmReply(conversation, content) ?: ChatIntents.replyFor(classification, GREETING)
		} catch (e: Exception) {
			log.warn("LLM reply failed for conversation ${conversation.id}, falling back to rule-based bot", e)
			ChatIntents.replyFor(classification, GREETING)
		}
	}

	/**
	 * Calls the Anthropic Messages API with the conversation history. Returns
	 * the assistant text or null when the provider returned nothing usable.
	 * Throws on HTTP errors / network failures so callers can fall back.
	 */
	private fun llmReply(conversation: ChatConversation, currentContent: String): String? {
		val model = settingService.chatbotModel()
		val maxTokens = settingService.chatbotMaxTokens()
		val history = messageRepository.findByConversationIdOrderByCreatedAt(conversation.id ?: 0L)
			.takeLast(MAX_LLM_HISTORY)
			.filter { it.sender != ChatSender.AGENT }
			.map { message ->
				message.sender.name.replace("CUSTOMER", "user").replace("ASSISTANT", "assistant") to message.content
			}

		val body = objectMapper.createObjectNode().apply {
			put("model", model)
			put("max_tokens", maxTokens)
			put("system", SYSTEM_PROMPT)
			set<ArrayNode>(
				"messages",
				objectMapper.createArrayNode().apply {
					history.forEach { (role, text) -> addObject().put("role", role).put("content", text) }
				},
			)
		}

		val raw = restClient.post()
			.uri(chatbotProperties.baseUrl.trimEnd('/') + "/v1/messages")
			.header("x-api-key", chatbotProperties.apiKey)
			.header("anthropic-version", "2023-06-01")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.retrieve()
			.onStatus({ status -> status.isError }) { _, response ->
				throw IOException("Anthropic returned HTTP ${response.statusCode.value()}")
			}
			.body(String::class.java)

		val root: JsonNode = objectMapper.readTree(raw)
		return (root["content"] as? ArrayNode)?.elements()?.asSequence()
			?.firstOrNull { it.path("type").asText() == "text" }
			?.path("text")
			?.takeIf { it.isTextual }
			?.asText()
			?.trim()
			?.takeUnless { it.isBlank() }
			?: throw IOException("Anthropic returned no text content")
	}

	companion object {
		private const val MODE_LIVE = "live"
		private const val MODE_DEMO = "demo"
		private const val MAX_CONCERN_SUMMARY = 500
		private const val MAX_LLM_HISTORY = 12

		private const val GREETING =
			"Hello! I'm the Britam claims assistant. I can help with claim updates, payouts, policy cover, " +
				"KYC verification and garage repairs. What would you like to know?"

		private const val SYSTEM_PROMPT =
			"You are the Britam motor insurance claims assistant, a friendly and concise support bot. " +
				"Help customers with claim progress, payouts, policy cover, KYC verification, garage repairs " +
				"and portal issues. Never invent claim numbers, amounts or dates the user has not given you - " +
				"ask for a claim number and point them to the portal when you need specific data. " +
				"When the user reports fraud, wishes to complain, or seems confused or frustrated, say clearly " +
				"that you have escalated the matter to the relevant Britam team and that someone will follow up. " +
				"Keep replies under 200 words and end with one question or next step."
	}
}