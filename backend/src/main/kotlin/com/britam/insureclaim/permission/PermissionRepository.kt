package com.britam.insureclaim.permission

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findByCodeIgnoreCase(code: String): Optional<Permission>
}
