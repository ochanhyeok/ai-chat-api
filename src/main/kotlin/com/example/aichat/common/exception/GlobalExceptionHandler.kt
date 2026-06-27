package com.example.aichat.common.exception

import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ErrorResponse> =
        build(ex.status, ex.message)

    /** DB 유니크 제약 위반 등(예: 피드백 중복 생성 race). 사용자에겐 409 로. */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        log.warn("데이터 무결성 위반", ex)
        return build(HttpStatus.CONFLICT, "요청이 데이터 제약 조건과 충돌합니다.")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors.joinToString(", ") {
            "${it.field}: ${it.defaultMessage}"
        }
        return build(HttpStatus.BAD_REQUEST, message.ifBlank { "Validation failed" })
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        build(HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다. 필수 필드를 확인하세요.")

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        // 내부 예외 메시지를 그대로 노출하지 않는다(정보 누출 방지). 상세는 로그로만.
        log.error("처리되지 않은 예외", ex)
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.")
    }

    private fun build(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(
                timestamp = Instant.now(),
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
            ),
        )
}
