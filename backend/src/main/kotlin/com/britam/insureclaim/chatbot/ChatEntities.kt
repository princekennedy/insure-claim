package com.britam.insureclaim.chatbot

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.common.BaseEntity
import com.britam.insureclaim.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.Instant

enum class ChatSender {
	CUSTOMER,
	ASSISTANT,
	AGENT,
}

enum class ChatConversationStatus {
	OPEN,
	ESCALATED,
	RESOLVED,
	CLOSED;

	val isActive: Boolean
		get() = this == OPEN || this == ESCALATED
}

/** Themes the chatbot classifies questions and escalations into. */
enum class ConcernCategory {
	CLAIM_DELAY,
	PAYOUT,
	KYC,
	GARAGE_QUALITY,
	POLICY,
	FRAUD_FLAG,
	COMPLAINT,
	TECHNICAL,
	OTHER;

	/** Whether the issue should reach a human instead of being auto-answered. */
	val needsHuman: Boolean
		get() = this == COMPLAINT || this == FRAUD_FLAG
}

enum class ConcernSeverity {
	LOW,
	MEDIUM,
	HIGH,
}

enum class ConcernStatus {
	OPEN,
	IN_PROGRESS,
	RESOLVED,
	DISMISSED;

	val isOpen: Boolean
		get() = this == OPEN || this == IN_PROGRESS
}

@Entity
@Table(name = "chat_conversations")
class ChatConversation(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	var user: User? = null,

	@Column(name = "reference", nullable = false, unique = true, length = 48)
	var reference: String = "",

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "claim_id")
	var claim: Claim? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	var status: ChatConversationStatus = ChatConversationStatus.OPEN,

	@Column(name = "messages_count", nullable = false)
	var messagesCount: Int = 0,

	@Column(name = "escalated", nullable = false)
	var escalated: Boolean = false,

	@Column(name = "escalated_at")
	var escalatedAt: Instant? = null,

	@Column(name = "started_at", nullable = false)
	var startedAt: Instant = Instant.now(),

	@Column(name = "ended_at")
	var endedAt: Instant? = null,

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.now(),
) : BaseEntity() {

	fun escalate(now: Instant = Instant.now()) {
		escalated = true
		status = ChatConversationStatus.ESCALATED
		escalatedAt = now
	}
}

@Entity
@Table(name = "chat_messages")
class ChatMessage(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conversation_id", nullable = false)
	var conversation: ChatConversation = ChatConversation(),

	@Enumerated(EnumType.STRING)
	@Column(name = "sender", nullable = false, length = 16)
	var sender: ChatSender = ChatSender.CUSTOMER,

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	var content: String = "",

	@Column(name = "intent", length = 48)
	var intent: String? = null,

	@Column(name = "confidence", precision = 5, scale = 4)
	var confidence: BigDecimal? = null,

	@Column(name = "escalate_suggested", nullable = false)
	var escalateSuggested: Boolean = false,

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.now(),
) : BaseEntity()

/**
 * A concern the chatbot surfaced to operations. This is the "capture and report
 * key concerns to the right team" requirement: the customer never has to write an
 * email because the conversation is already routed and triaged.
 */
@Entity
@Table(name = "support_concerns")
class SupportConcern(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conversation_id")
	var conversation: ChatConversation? = null,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	var user: User? = null,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "claim_id")
	var claim: Claim? = null,

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 32)
	var category: ConcernCategory = ConcernCategory.OTHER,

	@Enumerated(EnumType.STRING)
	@Column(name = "severity", nullable = false, length = 16)
	var severity: ConcernSeverity = ConcernSeverity.LOW,

	@Column(name = "summary", nullable = false, length = 500)
	var summary: String = "",

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	var status: ConcernStatus = ConcernStatus.OPEN,

	@Column(name = "assigned_to")
	var assignedTo: Long? = null,

	@Column(name = "resolved_at")
	var resolvedAt: Instant? = null,

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.now(),
) : BaseEntity()
