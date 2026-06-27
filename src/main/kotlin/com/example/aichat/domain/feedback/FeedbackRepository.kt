package com.example.aichat.domain.feedback

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface FeedbackRepository : JpaRepository<Feedback, UUID> {
    fun existsByUserIdAndChatId(userId: UUID, chatId: UUID): Boolean

    @Modifying
    @Query("delete from Feedback f where f.chatId in :chatIds")
    fun deleteByChatIdIn(chatIds: Collection<UUID>)

    // 본인 피드백 조회 (+ positive 필터)
    fun findByUserId(userId: UUID, pageable: Pageable): Page<Feedback>
    fun findByUserIdAndPositive(userId: UUID, positive: Boolean, pageable: Pageable): Page<Feedback>

    // 관리자 전체 조회 (+ positive 필터)
    fun findByPositive(positive: Boolean, pageable: Pageable): Page<Feedback>
}
