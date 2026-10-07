package com.britam.insureclaim.permission

import com.britam.insureclaim.common.AuditableVersionedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "permissions")
class Permission(
    @Column(name = "code", nullable = false, unique = true, length = 128)
    var code: String = "",

    @Column(name = "name", nullable = false, length = 160)
    var name: String = "",

    @Column(name = "description", length = 255)
    var description: String? = null,
) : AuditableVersionedEntity()


data class PermissionSummary(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
)

fun Permission.toSummary(): PermissionSummary = PermissionSummary(
    id = id ?: 0L,
    code = code,
    name = name,
    description = description,
)