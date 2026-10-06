package com.britam.insureclaim.claim

/**
 * Lifecycle of a claim from submission to settlement.
 *
 * The happy path is:
 * `SUBMITTED -> KYC_PENDING -> UNDER_REVIEW -> APPROVED -> GARAGE_ASSIGNED ->
 *  IN_REPAIR -> READY_FOR_PICKUP -> SETTLED -> CLOSED`
 *
 * Terminal states are `REJECTED`, `CLOSED` and `WITHDRAWN`. Transitions out of
 * terminal states are refused by [ClaimStatus.canTransitionTo].
 */
enum class ClaimStatus {
	DRAFT,
	SUBMITTED,
	KYC_PENDING,
	UNDER_REVIEW,
	FRAUD_CHECK,
	APPROVED,
	REJECTED,
	GARAGE_ASSIGNED,
	IN_REPAIR,
	READY_FOR_PICKUP,
	SETTLED,
	CLOSED,
	WITHDRAWN;

	val isTerminal: Boolean
		get() = this == REJECTED || this == CLOSED || this == WITHDRAWN

	/** Claims that still need insurer action. */
	val isOpen: Boolean
		get() = !isTerminal

	/** Claims where money is still expected to move. */
	val isAwaitingSettlement: Boolean
		get() = this == APPROVED || this == GARAGE_ASSIGNED || this == IN_REPAIR || this == READY_FOR_PICKUP

	val isCustomerVisibleLabel: String
		get() = when (this) {
			DRAFT -> "Draft"
			SUBMITTED -> "Submitted"
			KYC_PENDING -> "Awaiting KYC verification"
			UNDER_REVIEW -> "Under review"
			FRAUD_CHECK -> "Fraud screening"
			APPROVED -> "Approved"
			REJECTED -> "Rejected"
			GARAGE_ASSIGNED -> "Garage assigned"
			IN_REPAIR -> "Repair in progress"
			READY_FOR_PICKUP -> "Ready for pickup"
			SETTLED -> "Settled"
			CLOSED -> "Closed"
			WITHDRAWN -> "Withdrawn"
		}

	/** Ordering used to show progress as a percentage of the customer journey. */
	val journeyPosition: Int
		get() = when (this) {
			DRAFT -> 0
			SUBMITTED -> 1
			KYC_PENDING -> 2
			UNDER_REVIEW -> 3
			FRAUD_CHECK -> 4
			APPROVED -> 5
			GARAGE_ASSIGNED -> 6
			IN_REPAIR -> 7
			READY_FOR_PICKUP -> 8
			SETTLED -> 9
			CLOSED -> 10
			REJECTED -> 10
			WITHDRAWN -> 10
		}

	companion object {

		/**
		 * The full legal state machine. Anything not listed here is refused, so
		 * new endpoints cannot invent a transition by accident.
		 */
		val VALID_TRANSITIONS: Map<ClaimStatus, Set<ClaimStatus>> = mapOf(
			DRAFT to setOf(SUBMITTED, WITHDRAWN),
			SUBMITTED to setOf(KYC_PENDING, UNDER_REVIEW, WITHDRAWN, REJECTED),
			KYC_PENDING to setOf(UNDER_REVIEW, WITHDRAWN, REJECTED),
			UNDER_REVIEW to setOf(FRAUD_CHECK, REJECTED, APPROVED),
			FRAUD_CHECK to setOf(APPROVED, REJECTED),
APPROVED to setOf(GARAGE_ASSIGNED, REJECTED),
			GARAGE_ASSIGNED to setOf(IN_REPAIR, REJECTED),
			IN_REPAIR to setOf(READY_FOR_PICKUP),
			READY_FOR_PICKUP to setOf(SETTLED, IN_REPAIR),
			SETTLED to setOf(CLOSED),
			CLOSED to emptySet(),
			REJECTED to emptySet(),
			WITHDRAWN to emptySet(),
		)

		/** Transitions a customer may trigger on their own claim. */
		val CUSTOMER_TRANSITIONS: Map<ClaimStatus, Set<ClaimStatus>> = mapOf(
			DRAFT to setOf(SUBMITTED, WITHDRAWN),
			SUBMITTED to setOf(WITHDRAWN),
			KYC_PENDING to setOf(WITHDRAWN),
			READY_FOR_PICKUP to setOf(SETTLED),
		)

		fun canTransitionTo(from: ClaimStatus, to: ClaimStatus): Boolean =
			VALID_TRANSITIONS[from]?.contains(to) == true

		fun allowedNextStates(from: ClaimStatus): Set<ClaimStatus> = VALID_TRANSITIONS[from].orEmpty()
	}
}

enum class IncidentType {
	ACCIDENT,
	THEFT,
	FIRE,
	FLOOD,
	GLASS,
	WINDSCREEN,
	OTHER;

	val requiresPoliceReport: Boolean
		get() = this == ACCIDENT || this == THEFT
}

enum class ClaimDocumentType {
	DAMAGE_PHOTO,
	POLICE_REPORT,
	REPAIR_QUOTE,
	INVOICE,
	OWNERSHIP_DOCUMENT,
	RECEIPT,
	OTHER,
}
