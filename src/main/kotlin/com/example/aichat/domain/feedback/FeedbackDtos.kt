package com.example.aichat.domain.feedback

import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreateFeedbackRequest(
    @field:NotNull(message = "대화 ID는 필수입니다.")
    val chatId: UUID,

    @field:NotNull(message = "긍정/부정 여부는 필수입니다.")
    val positive: Boolean,
)

data class UpdateFeedbackStatusRequest(
    @field:NotNull(message = "상태는 필수입니다.")
    val status: FeedbackStatus,
)

data class FeedbackResponse(
    val id: UUID,
    val userId: UUID,
    val chatId: UUID,
    val positive: Boolean,
    val status: FeedbackStatus,
    val createdAt: Instant,
) {
    companion object {
        fun from(feedback: Feedback) = FeedbackResponse(
            id = feedback.id!!,
            userId = feedback.userId,
            chatId = feedback.chatId,
            positive = feedback.positive,
            status = feedback.status,
            createdAt = feedback.createdAt,
        )
    }
}
