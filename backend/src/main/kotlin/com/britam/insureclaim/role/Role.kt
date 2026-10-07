package com.britam.insureclaim.role

import com.britam.insureclaim.common.AuditableVersionedEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "roles")
class Role(
    @Column(name = "code", nullable = false, unique = true, length = 32)
    var code: String = "",

    @Column(name = "name", nullable = false, length = 160)
    var name: String = "",

    @Column(name = "description", length = 255)
    var description: String? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = [JoinColumn(name = "role_id")],
        inverseJoinColumns = [JoinColumn(name = "permission_id")]
    )
    var permissions: MutableSet<com.britam.insureclaim.permission.Permission> = mutableSetOf(),
) : AuditableVersionedEntity()
