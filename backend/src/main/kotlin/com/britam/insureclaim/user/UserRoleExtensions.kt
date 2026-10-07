package com.britam.insureclaim.user

import com.britam.insureclaim.role.Role

fun User.isStaff(): Boolean = role?.code in Role.STAFF_CODES
fun User.canViewAllClaims(): Boolean = role?.code in Role.STAFF_CODES
