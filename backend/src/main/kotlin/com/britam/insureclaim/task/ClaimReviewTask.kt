package com.britam.insureclaim.task

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.Optional
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

@Entity
@Table(name = "claim_review_tasks")
class ClaimReviewTask(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "claim_id", nullable = false, unique = true)
	var claim: Claim = Claim(),

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	var assignee: User? = null,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_by_id")
	var assignedBy: User? = null,

	@Column(name = "assigned_at")
	var assignedAt: Instant? = null,

	@Column(name = "created_at", nullable = false, updatable = false)
	var createdAt: Instant = Instant.now(),
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
}

@Entity
@Table(name = "claim_review_task_assignments")
class ClaimReviewTaskAssignment(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "task_id", nullable = false)
	var task: ClaimReviewTask = ClaimReviewTask(),

	@Column(name = "officer_id")
	var officerId: Long? = null,

	@Column(name = "actor_id")
	var actorId: Long? = null,

	@Column(name = "officer_label", nullable = false, length = 160)
	var officerLabel: String = "",

	@Column(name = "actor_label", nullable = false, length = 160)
	var actorLabel: String = "",

	@Column(name = "assigned_at", nullable = false)
	var assignedAt: Instant = Instant.now(),
) {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
}

interface ClaimReviewTaskRepository : JpaRepository<ClaimReviewTask, Long> {
	fun existsByClaimId(claimId: Long): Boolean

	@EntityGraph(attributePaths = ["claim", "claim.customer", "claim.vehicle", "assignee", "assignedBy"])
	fun findByClaimStatusInOrderByCreatedAtAsc(statuses: Collection<ClaimStatus>): List<ClaimReviewTask>

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT t FROM ClaimReviewTask t WHERE t.id = :id")
	fun findForUpdate(@Param("id") id: Long): Optional<ClaimReviewTask>
}

interface ClaimReviewTaskAssignmentRepository : JpaRepository<ClaimReviewTaskAssignment, Long>
