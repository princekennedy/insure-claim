package com.britam.insureclaim.fraud

import com.britam.insureclaim.claim.Claim
import com.britam.insureclaim.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

enum class FraudCategory {
	VELOCITY,
	AMOUNT,
	DOCUMENT,
	HISTORY,
	IDENTITY,
	BEHAVIOURAL,
	GARAGE,
}

enum class FraudSeverity {
	LOW,
	MEDIUM,
	HIGH,
	CRITICAL;

	/** Contribution to the 0-100 claim risk score. */
	val weight: Int
		get() = when (this) {
			LOW -> 5
			MEDIUM -> 15
			HIGH -> 30
			CRITICAL -> 50
		}

	val label: String
		get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class FraudAlertStatus {
	OPEN,
	UNDER_REVIEW,
	CONFIRMED,
	DISMISSED,
	RESOLVED;

	val isOpen: Boolean
		get() = this == OPEN || this == UNDER_REVIEW

	val isSettled: Boolean
		get() = this == DISMISSED || this == RESOLVED
}

@Entity
@Table(name = "fraud_alerts")
class FraudAlert(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "claim_id", nullable = false)
	var claim: Claim = Claim(),

	/** Stable identifier of the rule that fired, used to de-duplicate alerts. */
	@Column(name = "rule_code", nullable = false, length = 48)
	var ruleCode: String = "",

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 32)
	var category: FraudCategory = FraudCategory.VELOCITY,

	@Enumerated(EnumType.STRING)
	@Column(name = "severity", nullable = false, length = 16)
	var severity: FraudSeverity = FraudSeverity.LOW,

	@Column(name = "score_delta", nullable = false)
	var scoreDelta: Int = 0,

	@Column(name = "description", nullable = false, length = 500)
	var description: String = "",

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	var status: FraudAlertStatus = FraudAlertStatus.OPEN,

	@Column(name = "resolution", length = 500)
	var resolution: String? = null,

	@Column(name = "reviewed_by")
	var reviewedBy: Long? = null,

	@Column(name = "reviewed_at")
	var reviewedAt: Instant? = null,

	@Column(name = "created_at", nullable = false)
	var createdAt: Instant = Instant.now(),
) : BaseEntity()
