package com.example.aichat.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Authorization: Bearer <token> 헤더를 검사해 SecurityContext 에 인증 정보를 채운다.
 * 토큰이 없거나 유효하지 않으면 그냥 통과시키고, 보호된 엔드포인트는 SecurityConfig 가 거부한다.
 */
class JwtAuthenticationFilter(
    private val tokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)
        if (token != null) {
            runCatching { tokenProvider.parse(token) }
                .onSuccess { principal ->
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${principal.role.name}"))
                    val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
                    SecurityContextHolder.getContext().authentication = authentication
                }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.substring(7) else null
    }
}
