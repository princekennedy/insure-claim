package com.britam.insureclaim.chatbot

import com.britam.insureclaim.security.CurrentUserResolver
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Public chatbot widget endpoints plus staff read views for escalated concerns.
 * The widget calls [config] before it renders, then lazily creates a
 * conversation on first send. `POST /conversations`, `GET|POST` on a single
 * conversation are open so visitors can use the landing-page widget; when a
 * signed-in user calls them, ownership is enforced in [ChatService].
 */
@Tag(name = "Chat", description = "AI assistant conversations and escalated concerns")
@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
	private val chatService: ChatService,
	private val currentUser: CurrentUserResolver,
) {

	@Operation(summary = "Public widget configuration: enabled state, provider mode and greeting")
	@GetMapping("/config")
	fun config(): ChatConfigResponse = chatService.config()

	@Operation(summary = "Active conversations for the signed-in user (empty for visitors)")
	@GetMapping("/conversations")
	fun myConversations(): List<ConversationResponse> =
		chatService.myConversations(currentUser.currentOrNull()?.id)

	@Operation(summary = "Start a conversation, optionally bound to one of the caller's claims")
	@PostMapping("/conversations")
	@ResponseStatus(HttpStatus.CREATED)
	fun createConversation(@Valid @RequestBody request: CreateConversationRequest): ConversationResponse =
		chatService.createConversation(currentUser.currentOrNull()?.id, request)

	@Operation(summary = "Conversation with its full message history")
	@GetMapping("/conversations/{conversationId}")
	fun conversation(@PathVariable conversationId: Long): ConversationDetailResponse =
		chatService.conversationDetail(conversationId, currentUser.currentOrNull()?.id)

	@Operation(summary = "Send a message and get the assistant reply")
	@PostMapping("/conversations/{conversationId}/messages")
	fun sendMessage(
		@PathVariable conversationId: Long,
		@Valid @RequestBody request: SendMessageRequest,
	): ChatSendResponse = chatService.sendMessage(conversationId, currentUser.currentOrNull()?.id, request)

	@Operation(summary = "Concerns the chatbot surfaced to operations (staff only)")
	@GetMapping("/concerns")
	fun concerns(): List<SupportConcernResponse> = chatService.staffConcerns()
}