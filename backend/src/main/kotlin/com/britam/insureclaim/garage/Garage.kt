package com.britam.insureclaim.garage

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.claim.ClaimStatus
import com.britam.insureclaim.common.AuditableEntity
import com.britam.insureclaim.common.AuditableVersionedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

enum class GaragePerformanceStatus {
	GOOD,
	WATCH,
	UNDERPERFORMING,
	SUSPENDED;

	/** Only healthy and watched garages may be given new work. */
	val isAssignable: Boolean
		get() = this == GOOD || this == WATCH

	val label: String
		get() = when (this) {
			GOOD -> "Performing"
			WATCH -> "Under watch"
			UNDERPERFORMING -> "Underperforming"
			SUSPENDED -> "Suspended"
		}
}

enum class RepairJobStatus {
	ASSIGNED,
	ACCEPTED,
	IN_REPAIR,
	AWAITING_PARTS,
	QUALITY_CHECK,
	COMPLETED,
	DELIVERED,
	CANCELLED;

	val isFinished: Boolean
		get() = this == DELIVERED || this == CANCELLED

	/**
	 * Claim status this job state implies. Keeping the two in sync is what stops
	 * a customer seeing "in repair" while the claim still says "approved".
	 */
	val impliedClaimStatus: ClaimStatus?
		get() = when (this) {
			ASSIGNED -> ClaimStatus.GARAGE_ASSIGNED
			ACCEPTED, IN_REPAIR, AWAITING_PARTS -> ClaimStatus.IN_REPAIR
			QUALITY_CHECK, COMPLETED -> ClaimStatus.READY_FOR_PICKUP
			DELIVERED -> ClaimStatus.SETTLED
			CANCELLED -> null
		}

	val label: String
		get() = when (this) {
			ASSIGNED -> "Assigned"
			ACCEPTED -> "Accepted"
			IN_REPAIR -> "In repair"
			AWAITING_PARTS -> "Awaiting parts"
			QUALITY_CHECK -> "Quality check"
			COMPLETED -> "Repair completed"
			DELIVERED -> "Delivered"
			CANCELLED -> "Cancelled"
		}

	companion object {
		/** States a garage portal can move a job into. */
		val GARAGE_CONTROLLED: Set<RepairJobStatus> =
			setOf(ACCEPTED, IN_REPAIR, AWAITING_PARTS, QUALITY_CHECK, COMPLETED, DELIVERED)

		val VALID_TRANSITIONS: Map<RepairJobStatus, Set<RepairJobStatus>> = mapOf(
			ASSIGNED to setOf(ACCEPTED, CANCELLED),
			ACCEPTED to setOf(IN_REPAIR, CANCELLED),
			IN_REPAIR to setOf(AWAITING_PARTS, QUALITY_CHECK, CANCELLED),
			AWAITING_PARTS to setOf(IN_REPAIR, CANCELLED),
			QUALITY_CHECK to setOf(COMPLETED, IN_REPAIR),
			COMPLETED to setOf(DELIVERED),
			DELIVERED to emptySet(),
			CANCELLED to emptySet(),
		)

		fun canTransitionTo(from: RepairJobStatus, to: RepairJobStatus): Boolean =
			VALID_TRANSITIONS[from]?.contains(to) == true
	}
}

@Entity
@Table(name = "garages")
class Garage(
	@Column(name = "code", nullable = false, unique = true, length = 24)
	var code: String = "",

	@Column(name = "name", nullable = false, length = 160)
	var name: String = "",

	@Column(name = "address", nullable = false, length = 255)
	var address: String = "",

	@Column(name = "city", nullable = false, length = 80)
	var city: String = "",

	@Column(name = "contact_phone", nullable = false, length = 32)
	var contactPhone: String = "",

	@Column(name = "contact_email", length = 255)
	var contactEmail: String? = null,

	@Column(name = "panel_rating", precision = 3, scale = 2)
	var panelRating: BigDecimal? = null,

	/** Running mean of customer ratings, recomputed whenever feedback lands. */
	@Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
	var ratingAverage: BigDecimal = BigDecimal.ZERO.setScale(2),

	@Column(name = "rating_count", nullable = false)
	var ratingCount: Int = 0,

	@Column(name = "complaint_count", nullable = false)
	var complaintCount: Int = 0,

	@Column(name = "jobs_completed", nullable = false)
	var jobsCompleted: Int = 0,

	@Column(name = "avg_turnaround_days", precision = 6, scale = 2)
	var avgTurnaroundDays: BigDecimal? = null,

	@Column(name = "is_panel_garage", nullable = false)
	var isPanelGarage: Boolean = true,

	@Column(name = "active", nullable = false)
	var active: Boolean = true,

	@Enumerated(EnumType.STRING)
	@Column(name = "performance_status", nullable = false, length = 24)
	var performanceStatus: GaragePerformanceStatus = GaragePerformanceStatus.GOOD,

	@Column(name = "last_scored_at")
	var lastScoredAt: Instant? = null,
) : AuditableEntity() {

	fun recomputeRating(sumOfRatings: BigDecimal, count: Int) {
		ratingCount = count
		ratingAverage = if (count <= 0) {
			BigDecimal.ZERO.setScale(2)
		} else {
			sumOfRatings.divide(BigDecimal(count), 2, RoundingMode.HALF_UP)
		}
	}

	fun recordTurnaround(turnarounds: List<Duration>) {
		if (turnarounds.isEmpty()) return
		val averageDays = turnarounds.map { it.toHours() / 24.0 }.average()
		avgTurnaroundDays = BigDecimal(averageDays).setScale(2, RoundingMode.HALF_UP)
	}

	val isAcceptingWork: Boolean
		get() = active && performanceStatus.isAssignable

	fun fullAddress(): String = "$address, $city"
}

@Entity
@Table(name = "repair_jobs")
class RepairJob(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "claim_id", nullable = false)
	var claim: Claim = Claim(),

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "garage_id", nullable = false)
	var garage: Garage = Garage(),

	@Column(name = "reference_code", nullable = false, unique = true, length = 32)
	var referenceCode: String = "",

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	var status: RepairJobStatus = RepairJobStatus.ASSIGNED,

	@Column(name = "quoted_amount", precision = 14, scale = 2)
	var quotedAmount: BigDecimal? = null,

	@Column(name = "approved_amount", precision = 14, scale = 2)
	var approvedAmount: BigDecimal? = null,

	@Column(name = "final_amount", precision = 14, scale = 2)
	var finalAmount: BigDecimal? = null,

	@Column(name = "assigned_at", nullable = false)
	var assignedAt: Instant = Instant.now(),

	@Column(name = "started_at")
	var startedAt: Instant? = null,

	@Column(name = "completed_at")
	var completedAt: Instant? = null,

	@Column(name = "estimated_days")
	var estimatedDays: Int? = null,

	@Column(name = "warranty_days", nullable = false)
	var warrantyDays: Int = 90,

	@Column(name = "notes", length = 500)
	var notes: String? = null,
) : AuditableVersionedEntity() {

	fun turnaround(): Duration? {
		val start = startedAt ?: assignedAt
		val end = completedAt ?: return null
		return Duration.between(start, end)
	}

	fun progressPercent(): Int = when (status) {
		RepairJobStatus.ASSIGNED -> 5
		RepairJobStatus.ACCEPTED -> 15
		RepairJobStatus.IN_REPAIR -> 50
		RepairJobStatus.AWAITING_PARTS -> 40
		RepairJobStatus.QUALITY_CHECK -> 80
		RepairJobStatus.COMPLETED -> 92
		RepairJobStatus.DELIVERED -> 100
		RepairJobStatus.CANCELLED -> 0
	}
}
