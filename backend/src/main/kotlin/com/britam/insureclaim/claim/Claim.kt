package com.britam.insureclaim.claim

import com.britam.insureclaim.common.AuditableVersionedEntity
import com.britam.insureclaim.policy.Policy
import com.britam.insureclaim.user.User
import com.britam.insureclaim.vehicle.Vehicle
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Entity
@Table(name = "claims")
class Claim(
	@Column(name = "claim_number", nullable = false, unique = true, length = 48)
	var claimNumber: String = "",

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "policy_id", nullable = false)
	var policy: Policy = Policy().apply { id = 0L },

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	var customer: User = User().apply { id = 0L },

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	var vehicle: Vehicle = Vehicle().apply { id = 0L },

	@Enumerated(EnumType.STRING)
	@Column(name = "incident_type", nullable = false, length = 32)
	var incidentType: IncidentType = IncidentType.ACCIDENT,

	@Column(name = "incident_date", nullable = false)
	var incidentDate: Instant = Instant.now(),

	@Column(name = "incident_location", length = 255)
	var incidentLocation: String? = null,

	@Column(name = "description", nullable = false, columnDefinition = "TEXT")
	var description: String = "",

	@Column(name = "estimated_amount", precision = 14, scale = 2)
	var estimatedAmount: BigDecimal? = null,

	@Column(name = "approved_amount", precision = 14, scale = 2)
	var approvedAmount: BigDecimal? = null,

	@Column(name = "excess_paid", nullable = false, precision = 12, scale = 2)
	var excessPaid: BigDecimal = BigDecimal.ZERO,

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	var status: ClaimStatus = ClaimStatus.SUBMITTED,

	@Column(name = "requires_kyc", nullable = false)
	var requiresKyc: Boolean = true,

	/** 0-100 heuristic risk score produced by the fraud ruleset. */
	@Column(name = "fraud_score", nullable = false)
	var fraudScore: Int = 0,

	@Column(name = "is_fraud_flagged", nullable = false)
	var isFraudFlagged: Boolean = false,

	@Column(name = "reported_by_police", nullable = false)
	var reportedByPolice: Boolean = false,

	@Column(name = "third_party_involved", nullable = false)
	var thirdPartyInvolved: Boolean = false,

	@Column(name = "submitted_at", nullable = false)
	var submittedAt: Instant = Instant.now(),

	@Column(name = "settled_at")
	var settledAt: Instant? = null,
) : AuditableVersionedEntity() {

	@OneToMany(mappedBy = "claim", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
	var statusEvents: MutableList<ClaimStatusEvent> = mutableListOf()

	@OneToMany(mappedBy = "claim", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
	var documents: MutableList<ClaimDocument> = mutableListOf()

	val isOpen: Boolean
		get() = status.isOpen

	/** Incident date expressed as a calendar day, matching how policies are dated. */
	val incidentLocalDate: LocalDate
		get() = incidentDate.atZone(ZoneOffset.UTC).toLocalDate()

	/**
	 * Net payout = approved amount minus the policy excess. Floored at zero so an
	 * assessor cannot produce a negative settlement.
	 */
	fun netSettlement(): BigDecimal {
		val approved = approvedAmount ?: return BigDecimal.ZERO.setScale(2)
		return approved.subtract(policy.excessAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
	}

	fun addStatusEvent(
		from: ClaimStatus?,
		to: ClaimStatus,
		note: String?,
		actorId: Long?,
		actorLabel: String,
		visibleToCustomer: Boolean = true,
		occurredAt: Instant = Instant.now(),
	) {
		statusEvents += ClaimStatusEvent(
			claim = this,
			fromStatus = from,
			toStatus = to,
			note = note?.take(500),
			actorId = actorId,
			actorLabel = actorLabel,
			occurredAt = occurredAt,
			visibleToCustomer = visibleToCustomer,
		)
	}

	fun addDocument(document: ClaimDocument) {
		document.claim = this
		documents += document
	}

	fun removeDocument(document: ClaimDocument) {
		documents -= document
	}
}
