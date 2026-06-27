package com.example.aichat.domain.chat

import java.util.UUID

/**
 * 스레드(및 그 대화)가 삭제될 때 발행된다.
 * 피드백 등 대화를 참조하는 다른 모듈이 구독해 정리한다.
 * (chat → feedback 직접 의존을 만들지 않기 위한 이벤트 기반 분리)
 */
data class ThreadDeletedEvent(
    val threadId: UUID,
    val chatIds: List<UUID>,
)
