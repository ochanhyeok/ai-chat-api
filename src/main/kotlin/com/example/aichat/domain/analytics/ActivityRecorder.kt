package com.example.aichat.domain.analytics

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 활동 이벤트를 기록하는 진입점. 다른 도메인(auth/chat)이 의존한다.
 */
@Service
class ActivityRecorder(
    private val repository: ActivityLogRepository,
) {
    @Transactional
    fun record(userId: UUID, type: ActivityType) {
        repository.save(ActivityLog(userId = userId, type = type))
    }
}
