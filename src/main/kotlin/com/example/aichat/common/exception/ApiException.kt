package com.example.aichat.common.exception

import org.springframework.http.HttpStatus

/**
 * 서비스 계층에서 던지는 도메인 예외. GlobalExceptionHandler 가 HTTP 응답으로 변환한다.
 */
class ApiException(
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message) {

    companion object {
        fun badRequest(message: String) = ApiException(HttpStatus.BAD_REQUEST, message)
        fun unauthorized(message: String) = ApiException(HttpStatus.UNAUTHORIZED, message)
        fun forbidden(message: String) = ApiException(HttpStatus.FORBIDDEN, message)
        fun notFound(message: String) = ApiException(HttpStatus.NOT_FOUND, message)
        fun conflict(message: String) = ApiException(HttpStatus.CONFLICT, message)
    }
}
