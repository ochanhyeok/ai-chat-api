package com.example.aichat.domain.analytics

import java.time.Instant

/**
 * 요청 시점으로부터 24시간 동안의 사용자 활동 집계.
 */
data class ActivitySummaryResponse(
    val from: Instant,
    val to: Instant,
    val signupCount: Long,
    val loginCount: Long,
    val chatCount: Long,
)
