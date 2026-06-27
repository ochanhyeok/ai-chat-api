package com.example.aichat.domain.analytics

import com.example.aichat.common.exception.ApiException
import com.example.aichat.common.security.UserPrincipal
import com.example.aichat.domain.chat.ChatRepository
import com.example.aichat.domain.chat.ChatThreadRepository
import com.example.aichat.domain.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class AnalyticsService(
    private val activityLogRepository: ActivityLogRepository,
    private val chatRepository: ChatRepository,
    private val threadRepository: ChatThreadRepository,
    private val userRepository: UserRepository,
) {
    private val window = Duration.ofHours(24)

    /** 최근 24시간 회원가입/로그인/대화생성 수. 관리자 전용. */
    @Transactional(readOnly = true)
    fun activitySummary(principal: UserPrincipal): ActivitySummaryResponse {
        requireAdmin(principal)
        val now = Instant.now()
        val since = now.minus(window)
        return ActivitySummaryResponse(
            from = since,
            to = now,
            signupCount = activityLogRepository.countByTypeAndCreatedAtAfter(ActivityType.SIGNUP, since),
            loginCount = activityLogRepository.countByTypeAndCreatedAtAfter(ActivityType.LOGIN, since),
            chatCount = activityLogRepository.countByTypeAndCreatedAtAfter(ActivityType.CHAT_CREATED, since),
        )
    }

    /** 최근 24시간 전체 대화 목록 CSV. 어떤 사용자가 생성했는지 포함. 관리자 전용. */
    @Transactional(readOnly = true)
    fun generateReportCsv(principal: UserPrincipal): String {
        requireAdmin(principal)
        val since = Instant.now().minus(window)
        val chats = chatRepository.findByCreatedAtAfterOrderByCreatedAtAsc(since)

        val threadsById = threadRepository.findAllById(chats.map { it.threadId }.distinct())
            .associateBy { it.id }
        val usersById = userRepository.findAllById(threadsById.values.map { it.userId }.distinct())
            .associateBy { it.id }

        val sb = StringBuilder()
        sb.append("chat_id,user_id,user_email,user_name,thread_id,model,created_at,question,answer\n")
        for (chat in chats) {
            val thread = threadsById[chat.threadId]
            val user = thread?.let { usersById[it.userId] }
            val row = listOf(
                chat.id.toString(),
                user?.id?.toString() ?: "",
                user?.email ?: "",
                user?.name ?: "",
                chat.threadId.toString(),
                chat.model ?: "",
                chat.createdAt.toString(),
                chat.question,
                chat.answer,
            )
            sb.append(row.joinToString(",") { escapeCsv(it) }).append("\n")
        }
        return sb.toString()
    }

    private fun requireAdmin(principal: UserPrincipal) {
        if (!principal.isAdmin) throw ApiException.forbidden("관리자만 접근할 수 있습니다.")
    }

    /**
     * RFC 4180 식 CSV 이스케이프 + 수식 인젝션 방어.
     * - 쉼표/따옴표/개행 포함 시 따옴표로 감싸고 따옴표는 중복.
     * - =,+,-,@ 로 시작하면 스프레드시트가 수식으로 실행할 수 있어 앞에 ' 를 붙여 무력화.
     */
    private fun escapeCsv(value: String): String {
        val sanitized = if (value.isNotEmpty() && value.first() in charArrayOf('=', '+', '-', '@')) {
            "'$value"
        } else {
            value
        }
        val needsQuote = sanitized.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = sanitized.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }
}
