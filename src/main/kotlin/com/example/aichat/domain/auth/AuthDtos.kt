package com.example.aichat.domain.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,

    @field:NotBlank(message = "패스워드는 필수입니다.")
    @field:Size(min = 8, message = "패스워드는 8자 이상이어야 합니다.")
    val password: String,

    @field:NotBlank(message = "이름은 필수입니다.")
    val name: String,
)

data class LoginRequest(
    @field:Email(message = "올바른 이메일 형식이 아닙니다.")
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,

    @field:NotBlank(message = "패스워드는 필수입니다.")
    val password: String,
)

data class SignupResponse(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
)
