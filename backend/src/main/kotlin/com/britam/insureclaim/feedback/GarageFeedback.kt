package com.britam.insureclaim.feedback

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.common.BaseEntity
import com.britam.insureclaim.garage.Garage
import com.britam.insureclaim.garage.RepairJob
import com.britam.insureclaim.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * Customer rating of a completed repair. One per claim — a unique index on
 * `claim_id` enforces that a customer cannot review the same repair twice.
 */
@Entity
@Table(name = "garage_feedback")
class GarageFeedback(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "claim_id", nullable = false)
	var claim: Claim = Claim().apply { id = 0L },

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "garage_id", nullable = false)
	var garage: Garage = Garage().apply { id = 0L },

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "repair_job_id")
	var repairJob: RepairJob? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	var customer: User = User().apply { id = 0L },

	@Column(name = "overall_rating", nullable = false, precision = 3, scale = 2)
	var overallRating: BigDecimal = BigDecimal.ZERO.setScale(2),

	@Column(name = "quality_rating", precision = 3, scale = 2)
	var qualityRating: BigDecimal? = null,

	@Column(name = "timeliness_rating", precision = 3, scale = 2)
	var timelinessRating: BigDecimal? = null,

	@Column(name = "price_fairness_rating", precision = 3, scale = 2)
	var priceFairnessRating: BigDecimal? = null,

	@Column(name = "staff_courtesy_rating", precision = 3, scale = 2)
	var staffCourtesyRating: BigDecimal? = null,

	@Column(name = "comments", length = 1000)
	var comments: String? = null,

	@Column(name = "recommend_again")
	var recommendAgain: Boolean? = null,

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.now(),
) : BaseEntity() {

	/** A score of 2 or lower, or an explicit "would not recommend", counts as a complaint. */
	val isComplaint: Boolean
		get() = overallRating <= BigDecimal("2.00") || recommendAgain == false
}
