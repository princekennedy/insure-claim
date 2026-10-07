package com.britam.insureclaim.role

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByCodeIgnoreCase(code: String): Optional<Role>

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.code = :code")
    fun findByCodeWithPermissions(code: String): Optional<Role>
}
