package com.britam.insureclaim.policy

import com.britam.insureclaim.common.AuditableEntity
import com.britam.insureclaim.user.User
import com.britam.insureclaim.vehicle.Vehicle
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PolicyStatus {
	ACTIVE,
	EXPIRED,
	CANCELLED,
	LAPSED;

	companion object {
		/**
		 * Effective status given today's date. Kept in sync with the scheduled
		 * housekeeping job that flips rows to EXPIRED.
		 */
		fun effective(status: PolicyStatus, endDate: LocalDate, today: LocalDate = LocalDate.now()): PolicyStatus =
			if (status == ACTIVE && endDate.isBefore(today)) EXPIRED else status
	}
}

@Entity
@Table(name = "policies")
class Policy(
	@Column(name = "policy_number", nullable = false, unique = true, length = 48)
	var policyNumber: String = "",

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	var customer: User = User().apply { id = 0L },

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	var vehicle: Vehicle = Vehicle().apply { id = 0L },

	@Column(name = "insurer_name", nullable = false, length = 160)
	var insurerName: String = DEFAULT_INSURER,

	@Column(name = "product_code", nullable = false, length = 64)
	var productCode: String = DEFAULT_PRODUCT,

	@Column(name = "start_date", nullable = false)
	var startDate: LocalDate = LocalDate.now(),

	@Column(name = "end_date", nullable = false)
	var endDate: LocalDate = LocalDate.now().plusYears(1),

	@Column(name = "premium_amount", nullable = false, precision = 12, scale = 2)
	var premiumAmount: BigDecimal = BigDecimal.ZERO,

	@Column(name = "sum_insured", nullable = false, precision = 14, scale = 2)
	var sumInsured: BigDecimal = BigDecimal.ZERO,

	@Column(name = "excess_amount", nullable = false, precision = 12, scale = 2)
	var excessAmount: BigDecimal = BigDecimal.ZERO,

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 24)
	var status: PolicyStatus = PolicyStatus.ACTIVE,
) : AuditableEntity() {

	fun isCurrentlyValid(today: LocalDate = LocalDate.now()): Boolean =
		status == PolicyStatus.ACTIVE && !today.isBefore(startDate) && !today.isAfter(endDate)

	/** A claim can only be filed if the incident falls inside the policy window. */
	fun coversIncidentDate(incidentDate: LocalDate): Boolean =
		!incidentDate.isBefore(startDate) && !incidentDate.isAfter(endDate)

	fun daysUntilExpiry(today: LocalDate = LocalDate.now()): Long =
		ChronoUnit.DAYS.between(today, endDate)

	companion object {
		const val DEFAULT_INSURER = "Britam Insurance PLC"
		const val DEFAULT_PRODUCT = "MOTOR_COMPREHENSIVE"
	}
}
