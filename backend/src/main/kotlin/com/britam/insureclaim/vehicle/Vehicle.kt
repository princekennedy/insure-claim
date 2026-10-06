package com.britam.insureclaim.vehicle

import com.britam.insureclaim.common.AuditableEntity
import com.britam.insureclaim.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "vehicles")
class Vehicle(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	var owner: User = User(),

	@Column(name = "registration_number", nullable = false, unique = true, length = 32)
	var registrationNumber: String = "",

	@Column(name = "make", nullable = false, length = 80)
	var make: String = "",

	@Column(name = "model", nullable = false, length = 80)
	var model: String = "",

	@Column(name = "year", nullable = false)
	var year: Int = 0,

	@Column(name = "color", length = 40)
	var color: String? = null,

	@Column(name = "chassis_number", length = 64)
	var chassisNumber: String? = null,

	@Column(name = "engine_number", length = 64)
	var engineNumber: String? = null,
) : AuditableEntity() {

	val ageYears: Int
		get() = LocalDate.now().year - year

	fun displayName(): String = "$make $model ($year)"
}
