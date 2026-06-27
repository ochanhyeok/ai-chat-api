package com.example.aichat.domain.chat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 단일 질문-답변 쌍. (Chat:Thread = N:1)
 */
@Entity
@Table(name = "chat")
class Chat(
    @Column(name = "thread_id", nullable = false)
    val threadId: UUID,

    @Lob
    @Column(nullable = false)
    val question: String,

    @Lob
    @Column(nullable = false)
    val answer: String,

    @Column(name = "model")
    val model: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
)
