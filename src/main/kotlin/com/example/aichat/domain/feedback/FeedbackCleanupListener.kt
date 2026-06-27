package com.example.aichat.domain.feedback

import com.example.aichat.domain.chat.ThreadDeletedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 스레드(및 대화) 삭제 시 그 대화를 참조하던 피드백을 함께 정리한다.
 * 기본 @EventListener 는 발행 트랜잭션 내에서 동기 실행되므로, 삭제가 같은 트랜잭션에 묶인다.
 */
@Component
class FeedbackCleanupListener(
    private val feedbackRepository: FeedbackRepository,
) {
    @EventListener
    @Transactional
    fun onThreadDeleted(event: ThreadDeletedEvent) {
        if (event.chatIds.isNotEmpty()) {
            feedbackRepository.deleteByChatIdIn(event.chatIds)
        }
    }
}
