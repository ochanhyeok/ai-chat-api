package com.example.aichat.domain.chat

import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class CreateChatRequest(
    @field:NotBlank(message = "질문은 필수입니다.")
    val question: String,

    /** true 면 SSE 스트리밍으로 응답한다. */
    val isStreaming: Boolean = false,

    /** 지정 시 해당 모델로 응답을 생성한다. 미지정 시 기본 모델. */
    val model: String? = null,
)

data class ChatResponse(
    val id: UUID,
    val threadId: UUID,
    val question: String,
    val answer: String,
    val model: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(chat: Chat) = ChatResponse(
            id = chat.id!!,
            threadId = chat.threadId,
            question = chat.question,
            answer = chat.answer,
            model = chat.model,
            createdAt = chat.createdAt,
        )
    }
}

/** 스레드 단위로 그룹화된 대화 목록의 한 요소. */
data class ThreadResponse(
    val threadId: UUID,
    val createdAt: Instant,
    val chats: List<ChatResponse>,
)
