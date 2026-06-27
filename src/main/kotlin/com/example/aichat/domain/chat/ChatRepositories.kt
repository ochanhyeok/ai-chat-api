package com.example.aichat.domain.chat

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface ChatThreadRepository : JpaRepository<ChatThread, UUID> {
    /** 30분 세션 판정용: 유저의 가장 최근 스레드. */
    fun findFirstByUserIdOrderByLastChatAtDesc(userId: UUID): ChatThread?

    fun findByUserId(userId: UUID, pageable: Pageable): Page<ChatThread>
}

interface ChatRepository : JpaRepository<Chat, UUID> {
    fun findByThreadIdInOrderByCreatedAtAsc(threadIds: Collection<UUID>): List<Chat>

    @Modifying
    @Query("delete from Chat c where c.threadId = :threadId")
    fun deleteByThreadId(threadId: UUID)

    /** CSV 보고서용: 기간 내 전체 대화. */
    fun findByCreatedAtAfterOrderByCreatedAtAsc(after: Instant): List<Chat>
}
