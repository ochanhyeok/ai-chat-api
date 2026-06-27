package com.example.aichat.domain.feedback

import com.example.aichat.common.exception.ApiException
import com.example.aichat.common.security.UserPrincipal
import com.example.aichat.domain.chat.ChatRepository
import com.example.aichat.domain.chat.ChatThreadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val chatRepository: ChatRepository,
    private val threadRepository: ChatThreadRepository,
) {

    /** 피드백 생성. 멤버는 자신의 대화에만, 관리자는 모든 대화에. 대화당 사용자별 1개. */
    @Transactional
    fun create(principal: UserPrincipal, request: CreateFeedbackRequest): FeedbackResponse {
        val chat = chatRepository.findById(request.chatId).orElseThrow {
            ApiException.notFound("대화를 찾을 수 없습니다.")
        }
        if (!principal.isAdmin) {
            val thread = threadRepository.findById(chat.threadId).orElseThrow {
                ApiException.notFound("대화를 찾을 수 없습니다.")
            }
            if (thread.userId != principal.userId) {
                throw ApiException.forbidden("자신의 대화에만 피드백을 생성할 수 있습니다.")
            }
        }
        if (feedbackRepository.existsByUserIdAndChatId(principal.userId, request.chatId)) {
            throw ApiException.conflict("이미 이 대화에 피드백을 생성했습니다.")
        }
        val saved = feedbackRepository.save(
            Feedback(
                userId = principal.userId,
                chatId = request.chatId,
                positive = request.positive,
            ),
        )
        return FeedbackResponse.from(saved)
    }

    /** 피드백 목록. 멤버는 본인 것만, 관리자는 전체. positive 필터 + 정렬/페이지네이션. */
    @Transactional(readOnly = true)
    fun list(principal: UserPrincipal, positive: Boolean?, pageable: Pageable): Page<FeedbackResponse> {
        val page = when {
            principal.isAdmin && positive == null -> feedbackRepository.findAll(pageable)
            principal.isAdmin -> feedbackRepository.findByPositive(positive!!, pageable)
            positive == null -> feedbackRepository.findByUserId(principal.userId, pageable)
            else -> feedbackRepository.findByUserIdAndPositive(principal.userId, positive, pageable)
        }
        return page.map { FeedbackResponse.from(it) }
    }

    /** 피드백 상태 변경. 관리자만 가능. */
    @Transactional
    fun updateStatus(principal: UserPrincipal, feedbackId: UUID, status: FeedbackStatus): FeedbackResponse {
        if (!principal.isAdmin) {
            throw ApiException.forbidden("관리자만 상태를 변경할 수 있습니다.")
        }
        val feedback = feedbackRepository.findById(feedbackId).orElseThrow {
            ApiException.notFound("피드백을 찾을 수 없습니다.")
        }
        feedback.status = status
        return FeedbackResponse.from(feedback)
    }
}
