package com.example.aichat.common.config

import com.example.aichat.domain.user.Role
import com.example.aichat.domain.user.User
import com.example.aichat.domain.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 데모용 시드 계정 생성. app.seed.enabled=true 일 때만 동작한다.
 * H2 in-memory 라 재시작마다 동일 계정이 보장된다.
 *
 * - admin@demo.com  / admin1234  (ADMIN)
 * - member@demo.com / member1234 (MEMBER)
 */
@Configuration
@ConditionalOnProperty(prefix = "app.seed", name = ["enabled"], havingValue = "true")
class SeedDataInitializer {

    private val log = LoggerFactory.getLogger(SeedDataInitializer::class.java)

    @Bean
    fun seedRunner(
        userRepository: UserRepository,
        passwordEncoder: PasswordEncoder,
    ): ApplicationRunner = ApplicationRunner {
        seedUser(userRepository, passwordEncoder, "admin@demo.com", "admin1234", "데모 관리자", Role.ADMIN)
        seedUser(userRepository, passwordEncoder, "member@demo.com", "member1234", "데모 멤버", Role.MEMBER)
        log.info("시드 계정 준비 완료 (admin@demo.com / member@demo.com)")
    }

    private fun seedUser(
        repo: UserRepository,
        encoder: PasswordEncoder,
        email: String,
        rawPassword: String,
        name: String,
        role: Role,
    ) {
        if (repo.existsByEmail(email)) return
        repo.save(
            User(
                email = email,
                password = encoder.encode(rawPassword),
                name = name,
                role = role,
            ),
        )
    }
}
