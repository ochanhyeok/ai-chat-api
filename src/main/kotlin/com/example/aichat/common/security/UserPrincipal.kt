package com.example.aichat.common.security

import com.example.aichat.domain.user.Role
import java.util.UUID

/**
 * 인증된 사용자 정보. SecurityContext 의 principal 로 보관된다.
 */
data class UserPrincipal(
    val userId: UUID,
    val email: String,
    val role: Role,
) {
    val isAdmin: Boolean get() = role == Role.ADMIN
}
