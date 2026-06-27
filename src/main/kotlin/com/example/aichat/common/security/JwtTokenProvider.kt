package com.example.aichat.common.security

import com.example.aichat.common.config.AppProperties
import com.example.aichat.domain.user.Role
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(props: AppProperties) {

    private val key: SecretKey = Keys.hmacShaKeyFor(props.jwt.secret.toByteArray())
    private val expirationMillis: Long = props.jwt.expirationMillis

    fun createToken(userId: UUID, email: String, role: Role): String {
        val now = Date()
        val expiry = Date(now.time + expirationMillis)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role.name)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /** 토큰을 파싱해 UserPrincipal 로 변환한다. 유효하지 않으면 예외를 던진다. */
    fun parse(token: String): UserPrincipal {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
        return UserPrincipal(
            userId = UUID.fromString(claims.subject),
            email = claims["email"] as String,
            role = Role.valueOf(claims["role"] as String),
        )
    }
}
