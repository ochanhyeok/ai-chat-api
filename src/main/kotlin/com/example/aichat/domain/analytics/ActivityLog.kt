package com.example.aichat.domain.analytics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class ActivityType {
    SIGNUP,
    LOGIN,
    CHAT_CREATED,
}

/**
 * 사용자 활동 이벤트 기록. signup/login/chat 생성을 한 테이블에 통일 기록하여
 * 24시간 집계를 단일 쿼리로 처리한다. (특히 login 은 다른 테이블에 남지 않으므로 필수)
 */
@Entity
@Table(name = "activity_log")
class ActivityLog(
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: ActivityType,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
)
