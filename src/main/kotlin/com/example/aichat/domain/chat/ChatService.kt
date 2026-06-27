package com.example.aichat.domain.chat

import com.example.aichat.common.exception.ApiException
import com.example.aichat.common.security.UserPrincipal
import com.example.aichat.infra.llm.LlmMessage
import com.example.aichat.infra.llm.LlmProvider
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant

@Service
class ChatService(
    private val chatStore: ChatStore,
    private val threadRepository: ChatThreadRepository,
    private val chatRepository: ChatRepository,
    private val llmProvider: LlmProvider,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(ChatService::class.java)

    companion object {
        private const val SYSTEM_PROMPT = "You are a helpful assistant."
    }

    /** 스레드의 이전 대화 + 이번 질문으로 OpenAI messages 배열을 구성한다. */
    private fun buildMessages(priorMessages: List<LlmMessage>, question: String): List<LlmMessage> =
        listOf(LlmMessage(role = "system", content = SYSTEM_PROMPT)) +
            priorMessages +
            LlmMessage(role = "user", content = question)

    /** 동기(JSON) 대화 생성. */
    fun createChat(principal: UserPrincipal, request: CreateChatRequest): ChatResponse {
        val ctx = chatStore.prepareThread(principal.userId, Instant.now())
        val messages = buildMessages(ctx.priorMessages, request.question)
        val result = llmProvider.chat(messages, request.model)
        val chat = chatStore.persistAnswer(
            userId = principal.userId,
            threadId = ctx.threadId,
            question = request.question,
            answer = result.content,
            model = result.model,
        )
        return ChatResponse.from(chat)
    }

    /**
     * 스트리밍(SSE) 대화 생성.
     * - delta 이벤트로 부분 토큰을 흘려보내고
     * - 완료 시 done 이벤트로 영속화된 대화 전체를 전달한다.
     * 스트림이 끝난 뒤에야 전체 답변을 DB 에 저장한다.
     */
    fun createChatStream(principal: UserPrincipal, request: CreateChatRequest): SseEmitter {
        val emitter = SseEmitter(0L) // 타임아웃 없음
        val ctx = chatStore.prepareThread(principal.userId, Instant.now())
        val messages = buildMessages(ctx.priorMessages, request.question)
        val buffer = StringBuilder()

        val subscription = llmProvider.streamChat(messages, request.model).subscribe(
            { delta ->
                buffer.append(delta)
                runCatching { emitter.send(SseEmitter.event().name("delta").data(delta)) }
                    .onFailure { emitter.completeWithError(it) }
            },
            { error ->
                log.error("스트리밍 중 오류", error)
                emitter.completeWithError(error)
            },
            {
                runCatching {
                    val chat = chatStore.persistAnswer(
                        userId = principal.userId,
                        threadId = ctx.threadId,
                        question = request.question,
                        answer = buffer.toString(),
                        model = request.model,
                    )
                    emitter.send(SseEmitter.event().name("done").data(ChatResponse.from(chat)))
                    emitter.complete()
                }.onFailure { emitter.completeWithError(it) }
            },
        )
        // 클라이언트 연결 종료/타임아웃 시 업스트림 LLM 구독을 정리해 누수를 막는다.
        emitter.onCompletion { subscription.dispose() }
        emitter.onTimeout { subscription.dispose() }
        emitter.onError { subscription.dispose() }
        return emitter
    }

    /** 스레드 단위로 그룹화된 대화 목록. 멤버는 본인 것만, 관리자는 전체. */
    @Transactional(readOnly = true)
    fun listThreads(principal: UserPrincipal, pageable: Pageable): Page<ThreadResponse> {
        val threads = if (principal.isAdmin) {
            threadRepository.findAll(pageable)
        } else {
            threadRepository.findByUserId(principal.userId, pageable)
        }
        val threadIds = threads.content.mapNotNull { it.id }
        val chatsByThread = if (threadIds.isEmpty()) {
            emptyMap()
        } else {
            chatRepository.findByThreadIdInOrderByCreatedAtAsc(threadIds).groupBy { it.threadId }
        }
        return threads.map { thread ->
            ThreadResponse(
                threadId = thread.id!!,
                createdAt = thread.createdAt,
                chats = chatsByThread[thread.id].orEmpty().map { ChatResponse.from(it) },
            )
        }
    }

    /** 스레드 삭제. 본인이 생성한 스레드만 삭제 가능. */
    @Transactional
    fun deleteThread(principal: UserPrincipal, threadId: java.util.UUID) {
        val thread = threadRepository.findById(threadId).orElseThrow {
            ApiException.notFound("스레드를 찾을 수 없습니다.")
        }
        if (thread.userId != principal.userId) {
            throw ApiException.forbidden("자신의 스레드만 삭제할 수 있습니다.")
        }
        // 이 스레드의 대화를 참조하는 피드백을 정리하도록 이벤트를 발행한다(고아 방지).
        val chatIds = chatRepository.findByThreadIdInOrderByCreatedAtAsc(listOf(threadId)).mapNotNull { it.id }
        eventPublisher.publishEvent(ThreadDeletedEvent(threadId = threadId, chatIds = chatIds))
        chatRepository.deleteByThreadId(threadId)
        threadRepository.delete(thread)
    }
}
