package com.example.aichat.common.security

import com.example.aichat.common.exception.ApiException
import org.springframework.security.core.context.SecurityContextHolder

/**
 * 현재 요청의 인증 사용자에 접근하는 헬퍼.
 */
object SecurityUtils {

    fun currentPrincipalOrNull(): UserPrincipal? =
        SecurityContextHolder.getContext().authentication?.principal as? UserPrincipal

    fun currentPrincipal(): UserPrincipal =
        currentPrincipalOrNull() ?: throw ApiException.unauthorized("인증이 필요합니다.")
}
