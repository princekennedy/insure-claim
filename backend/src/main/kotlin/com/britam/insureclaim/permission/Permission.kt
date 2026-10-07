package com.britam.insureclaim.permission

/**
 * Permission codes are currently reserved for future use. The portal enforces
 * access via role codes (see [com.britam.insureclaim.role]) and Spring Security
 * @PreAuthorize / HttpSecurity rules rather than a permission table.
 */
class Permission

data class PermissionSummary(
    val code: String,
    val name: String,
    val description: String?,
)
