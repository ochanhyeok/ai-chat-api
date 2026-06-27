package com.example.aichat.domain.chat

import com.example.aichat.domain.analytics.ActivityRecorder
import com.example.aichat.domain.analytics.ActivityType
import com.example.aichat.infra.llm.LlmMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 스레드 해석/영속화의 트랜잭션 경계를 담당한다.
 * ChatService 의 스트리밍 콜백(별도 스레드)에서 호출되므로, self-invocation 으로
 * @Transactional 이 무효화되지 않도록 별도 빈으로 분리했다.
 */
@Service
class ChatStore(
    private val threadRepository: ChatThreadRepository,
    private val chatRepository: ChatRepository,
    private val activityRecorder: ActivityRecorder,
) {
    companion object {
        /** 마지막 질문 후 이 시간 이내면 기존 스레드 유지, 초과하면 새 스레드 생성. */
        val SESSION_WINDOW: Duration = Duration.ofMinutes(30)
    }

    data class ThreadContext(
        val threadId: UUID,
        val priorMessages: List<LlmMessage>,
    )

    /**
     * 세션 규칙에 따라 스레드를 해석한다.
     * - 유저의 최근 스레드가 없거나 마지막 질문 후 30분 초과 → 새 스레드
     * - 30분 이내 → 기존 스레드 유지(lastChatAt 갱신)
     * 그리고 해당 스레드의 이전 대화들을 LLM 메시지 목록으로 반환한다.
     */
    @Transactional
    fun prepareThread(userId: UUID, now: Instant): ThreadContext {
        val latest = threadRepository.findFirstByUserIdOrderByLastChatAtDesc(userId)
        val thread = if (latest != null && Duration.between(latest.lastChatAt, now) <= SESSION_WINDOW) {
            latest.lastChatAt = now
            latest
        } else {
            threadRepository.save(ChatThread(userId = userId, lastChatAt = now, createdAt = now))
        }
        val threadId = thread.id!!
        val priorMessages = chatRepository.findByThreadIdInOrderByCreatedAtAsc(listOf(threadId))
            .flatMap {
                listOf(
                    LlmMessage(role = "user", content = it.question),
                    LlmMessage(role = "assistant", content = it.answer),
                )
            }
        return ThreadContext(threadId = threadId, priorMessages = priorMessages)
    }

    @Transactional
    fun persistAnswer(userId: UUID, threadId: UUID, question: String, answer: String, model: String?): Chat {
        val chat = chatRepository.save(
            Chat(threadId = threadId, question = question, answer = answer, model = model),
        )
        activityRecorder.record(userId, ActivityType.CHAT_CREATED)
        return chat
    }
}
