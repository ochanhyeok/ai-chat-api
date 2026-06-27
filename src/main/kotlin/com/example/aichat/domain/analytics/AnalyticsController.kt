package com.example.aichat.domain.analytics

import com.example.aichat.common.security.UserPrincipal
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.Instant

@RestController
@RequestMapping("/api/admin/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService,
) {

    @GetMapping("/activity")
    fun activity(@AuthenticationPrincipal principal: UserPrincipal): ActivitySummaryResponse =
        analyticsService.activitySummary(principal)

    @GetMapping("/report", produces = ["text/csv"])
    fun report(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<String> {
        val csv = analyticsService.generateReportCsv(principal)
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"chat-report-$stamp.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv)
    }
}
