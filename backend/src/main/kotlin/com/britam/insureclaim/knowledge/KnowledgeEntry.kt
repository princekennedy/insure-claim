package com.britam.insureclaim.knowledge

import com.britam.insureclaim.common.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository

/**
 * One article in the assistant's company knowledge base. Articles are managed
 * as a catalogue (not a single settings blob) so operators can publish, edit and
 * retire individual pieces of Britam information.
 */
@Entity
@Table(name = "knowledge_entries")
class KnowledgeEntry(
	@Column(name = "title", nullable = false, length = 160)
	var title: String = "",

	@Column(name = "category", nullable = false, length = 64)
	var category: String = "General",

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	var content: String = "",

	@Column(name = "enabled", nullable = false)
	var enabled: Boolean = true,
) : AuditableEntity()

interface KnowledgeEntryRepository : JpaRepository<KnowledgeEntry, Long> {

	fun findAllByOrderByUpdatedAtDesc(): List<KnowledgeEntry>

	fun findAllByEnabledTrueOrderByCategoryAscTitleAsc(): List<KnowledgeEntry>
}
