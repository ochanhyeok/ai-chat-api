package com.example.aichat.domain.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 대화 스레드. 한 유저가 여러 개를 가질 수 있다. (Thread:User = N:1)
 * 클래스명은 java.lang.Thread 와의 혼동을 피하려고 ChatThread 로 둔다.
 *
 * lastChatAt 은 30분 세션 판정을 매번 max(chat.created_at) 으로 조회하지 않도록
 * 비정규화한 컬럼이다. 질문이 추가될 때마다 갱신된다.
 */
@Entity
@Table(name = "chat_thread")
class ChatThread(
    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "last_chat_at", nullable = false)
    var lastChatAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
)
