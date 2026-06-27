package com.example.aichat.domain.feedback

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

enum class FeedbackStatus {
    PENDING,
    RESOLVED,
}

/**
 * 대화에 대한 사용자 피드백.
 * (user_id, chat_id) 복합 유니크로 "한 사용자는 한 대화에 하나의 피드백" 규칙을 DB 가 보장한다.
 * 한 대화에는 서로 다른 사용자의 N개 피드백이 존재할 수 있다.
 */
@Entity
@Table(
    name = "feedback",
    uniqueConstraints = [UniqueConstraint(name = "uk_feedback_user_chat", columnNames = ["user_id", "chat_id"])],
)
class Feedback(
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "chat_id", nullable = false)
    val chatId: UUID,

    @Column(name = "is_positive", nullable = false)
    val positive: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FeedbackStatus = FeedbackStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
)
