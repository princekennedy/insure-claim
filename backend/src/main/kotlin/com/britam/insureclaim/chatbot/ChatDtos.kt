package com.britam.insureclaim.chatbot

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

@Schema(name = "ChatConfig", description = "Public chatbot configuration the widget reads before opening")
data class ChatConfigResponse(
	val enabled: Boolean,
	@param:Schema(description = "live when an LLM provider is configured, otherwise demo (rule-based)")
	val mode: String,
	val model: String?,
	val greeting: String,
)

@Schema(name = "Conversation", description = "One chatbot conversation")
data class ConversationResponse(
	val id: Long,
	val reference: String,
	val status: String,
	val escalated: Boolean,
	val messagesCount: Int,
	val startedAt: Instant,
	val escalatedAt: Instant?,
)

@Schema(name = "ChatMessage", description = "A single message inside a conversation")
data class MessageResponse(
	val id: Long,
	val sender: String,
	val content: String,
	val intent: String?,
	val confidence: Double?,
	val escalateSuggested: Boolean,
	val createdAt: Instant,
)

@Schema(name = "SupportConcern", description = "Issue the chatbot surfaced to operations")
data class SupportConcernResponse(
	val id: Long,
	val category: String,
	val severity: String,
	val summary: String,
	val status: String,
	val conversationId: Long?,
	val createdAt: Instant,
)

@Schema(name = "ConversationDetail", description = "A conversation with its full message history")
data class ConversationDetailResponse(
	val conversation: ConversationResponse,
	val messages: List<MessageResponse>,
)

@Schema(name = "ChatSendResult", description = "Result of sending one customer message")
data class ChatSendResponse(
	val conversation: ConversationResponse,
	val userMessage: MessageResponse,
	val assistantMessage: MessageResponse,
	val escalated: Boolean,
	val concern: SupportConcernResponse?,
)

@Schema(name = "CreateConversationRequest", description = "Optionally bind the conversation to one of the caller's claims")
data class CreateConversationRequest(
	@field:Schema(description = "Claim id the customer wants to discuss; validated against their own claims")
	val claimId: Long? = null,
)

@Schema(name = "SendMessageRequest")
data class SendMessageRequest(
	@field:NotBlank @field:Size(max = 4000) val content: String,
)

fun ConversationResponse.toResponse(conversation: ChatConversation) = ConversationResponse(
	id = conversation.id ?: 0L,
	reference = conversation.reference,
	status = conversation.status.name,
	escalated = conversation.escalated,
	messagesCount = conversation.messagesCount,
	startedAt = conversation.startedAt,
	escalatedAt = conversation.escalatedAt,
)

fun MessageResponse.toResponse(message: ChatMessage) = MessageResponse(
	id = message.id ?: 0L,
	sender = message.sender.name,
	content = message.content,
	intent = message.intent,
	confidence = message.confidence?.toDouble(),
	escalateSuggested = message.escalateSuggested,
	createdAt = message.createdAt,
)

fun SupportConcernResponse.toResponse(concern: SupportConcern) = SupportConcernResponse(
	id = concern.id ?: 0L,
	category = concern.category.name,
	severity = concern.severity.name,
	summary = concern.summary,
	status = concern.status.name,
	conversationId = concern.conversation?.id,
	createdAt = concern.createdAt,
)