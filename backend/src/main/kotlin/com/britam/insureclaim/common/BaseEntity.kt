package com.britam.insureclaim.common

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

/**
 * Surrogate identity for every entity. Kept deliberately minimal so that each
 * concrete table only inherits the columns it actually declares.
 */
@MappedSuperclass
abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is BaseEntity) return false
		val selfId = id ?: return false
		return selfId == other.id && javaClass == other.javaClass
	}

	override fun hashCode(): Int = id?.hashCode() ?: javaClass.hashCode()

	override fun toString(): String = "${javaClass.simpleName}(id=$id)"
}

/**
 * Adds optimistic locking so concurrent status transitions on the same claim or
 * repair job fail fast instead of silently overwriting each other.
 */
@MappedSuperclass
abstract class VersionedEntity : BaseEntity() {

	@Version
	@Column(name = "version", nullable = false)
	var version: Long = 0
}

@MappedSuperclass
abstract class AuditableEntity : BaseEntity() {

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	var createdAt: Instant = Instant.now()

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.now()
}

@MappedSuperclass
abstract class AuditableVersionedEntity : VersionedEntity() {

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	var createdAt: Instant = Instant.now()

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	var updatedAt: Instant = Instant.now()
}
