package com.example.aichat.domain.analytics

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface ActivityLogRepository : JpaRepository<ActivityLog, UUID> {
    fun countByTypeAndCreatedAtAfter(type: ActivityType, after: Instant): Long
}
