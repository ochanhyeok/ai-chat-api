package com.example.aichat.domain.chat

import com.example.aichat.domain.analytics.ActivityLogRepository
import com.example.aichat.domain.analytics.ActivityRecorder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * 30분 스레드 세션 규칙 검증. 이 과제에서 가장 까다로운 비즈니스 로직이다.
 * prepareThread 가 now 를 파라미터로 받게 설계해 시간 흐름을 결정적으로 테스트한다.
 */
@DataJpaTest
class ChatSessionTest {

    @Autowired
    lateinit var threadRepository: ChatThreadRepository

    @Autowired
    lateinit var chatRepository: ChatRepository

    @Autowired
    lateinit var activityLogRepository: ActivityLogRepository

    private lateinit var chatStore: ChatStore
    private val userId: UUID = UUID.randomUUID()
    private val t0: Instant = Instant.parse("2026-01-01T00:00:00Z")

    @BeforeEach
    fun setUp() {
        chatStore = ChatStore(threadRepository, chatRepository, ActivityRecorder(activityLogRepository))
    }

    @Test
    fun `첫 질문이면 새 스레드를 생성한다`() {
        val ctx = chatStore.prepareThread(userId, t0)
        assertThat(threadRepository.findAll()).hasSize(1)
        assertThat(ctx.priorMessages).isEmpty()
    }

    @Test
    fun `30분 이내 재질문이면 기존 스레드를 유지한다`() {
        val first = chatStore.prepareThread(userId, t0)
        chatStore.persistAnswer(userId, first.threadId, "q1", "a1", "m")

        val second = chatStore.prepareThread(userId, t0.plus(Duration.ofMinutes(29)))

        assertThat(second.threadId).isEqualTo(first.threadId)
        // 이전 대화(q1/a1)가 user/assistant 메시지로 함께 전달되어야 한다.
        assertThat(second.priorMessages.map { it.content }).containsExactly("q1", "a1")
    }

    @Test
    fun `정확히 30분 경계는 기존 스레드를 유지한다`() {
        val first = chatStore.prepareThread(userId, t0)
        chatStore.persistAnswer(userId, first.threadId, "q1", "a1", "m")

        val second = chatStore.prepareThread(userId, t0.plus(Duration.ofMinutes(30)))

        assertThat(second.threadId).isEqualTo(first.threadId)
    }

    @Test
    fun `30분 초과 후 질문이면 새 스레드를 생성한다`() {
        val first = chatStore.prepareThread(userId, t0)
        chatStore.persistAnswer(userId, first.threadId, "q1", "a1", "m")

        val second = chatStore.prepareThread(userId, t0.plus(Duration.ofMinutes(31)))

        assertThat(second.threadId).isNotEqualTo(first.threadId)
        assertThat(threadRepository.findAll()).hasSize(2)
        // 새 스레드이므로 이전 대화 컨텍스트는 비어 있어야 한다.
        assertThat(second.priorMessages).isEmpty()
    }
}
