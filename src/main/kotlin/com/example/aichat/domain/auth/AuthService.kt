package com.example.aichat.domain.auth

import com.example.aichat.common.exception.ApiException
import com.example.aichat.common.security.JwtTokenProvider
import com.example.aichat.domain.analytics.ActivityRecorder
import com.example.aichat.domain.analytics.ActivityType
import com.example.aichat.domain.user.Role
import com.example.aichat.domain.user.User
import com.example.aichat.domain.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenProvider: JwtTokenProvider,
    private val activityRecorder: ActivityRecorder,
) {

    @Transactional
    fun signup(request: SignupRequest): SignupResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ApiException.conflict("이미 사용 중인 이메일입니다.")
        }
        // 보안상 가입은 항상 MEMBER 로 고정한다. ADMIN 은 시드/운영 경로로만 생성.
        val user = userRepository.save(
            User(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                name = request.name,
                role = Role.MEMBER,
            ),
        )
        activityRecorder.record(user.id!!, ActivityType.SIGNUP)
        return SignupResponse(
            id = user.id.toString(),
            email = user.email,
            name = user.name,
            role = user.role.name,
        )
    }

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw ApiException.unauthorized("이메일 또는 패스워드가 올바르지 않습니다.")
        if (!passwordEncoder.matches(request.password, user.password)) {
            throw ApiException.unauthorized("이메일 또는 패스워드가 올바르지 않습니다.")
        }
        val userId = user.id!!
        activityRecorder.record(userId, ActivityType.LOGIN)
        val token = tokenProvider.createToken(userId, user.email, user.role)
        return LoginResponse(accessToken = token)
    }
}
