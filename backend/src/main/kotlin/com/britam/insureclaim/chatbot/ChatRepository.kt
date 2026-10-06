package com.britam.insureclaim.chatbot

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ChatConversationRepository : JpaRepository<ChatConversation, Long> {

	fun findByReference(reference: String): Optional<ChatConversation>

	fun findByUserIdAndStatusInOrderByStartedAtDesc(
		userId: Long,
		statuses: Collection<ChatConversationStatus>,
		pageable: Pageable,
	): Page<ChatConversation>

	fun countByStatus(status: ChatConversationStatus): Long

	@Query(
		"""
		SELECT c FROM ChatConversation c
		WHERE c.escalated = true AND c.status = :status
		ORDER BY c.escalatedAt DESC
		""",
	)
	fun findEscalated(@Param("status") status: ChatConversationStatus, pageable: Pageable): Page<ChatConversation>

	@Query("SELECT FUNCTION('date_trunc', 'day', c.startedAt), COUNT(c) FROM ChatConversation c GROUP BY FUNCTION('date_trunc', 'day', c.startedAt) ORDER BY FUNCTION('date_trunc', 'day', c.startedAt)")
	fun countPerDay(): List<Array<Any>>
}

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
	fun findByConversationIdOrderByCreatedAt(conversationId: Long): List<ChatMessage>
	fun countByEscalateSuggestedTrue(): Long
}

interface SupportConcernRepository : JpaRepository<SupportConcern, Long> {

	fun findByStatusOrderByCreatedAtDesc(status: ConcernStatus, pageable: Pageable): Page<SupportConcern>

	fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<SupportConcern>

	fun countByStatus(status: ConcernStatus): Long

	@Query("SELECT c.category, COUNT(c) FROM SupportConcern c GROUP BY c.category")
	fun countGroupedByCategory(): List<Array<Any>>

	@Query("SELECT c.status, COUNT(c) FROM SupportConcern c GROUP BY c.status")
	fun countGroupedByStatus(): List<Array<Any>>
}
