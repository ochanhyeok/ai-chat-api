package com.example.aichat.domain.chat

import com.example.aichat.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val chatService: ChatService,
) {

    /**
     * 대화 생성. isStreaming=true 면 SSE(text/event-stream)로, 아니면 JSON 으로 응답한다.
     * 반환 타입이 분기되므로 Any 로 선언한다.
     */
    @PostMapping
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateChatRequest,
    ): Any =
        if (request.isStreaming) {
            chatService.createChatStream(principal, request)
        } else {
            chatService.createChat(principal, request)
        }

    /** 스레드 단위로 그룹화된 대화 목록. 정렬(asc/desc) + 페이지네이션. */
    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "desc") sort: String,
    ): Page<ThreadResponse> {
        val direction = if (sort.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by(direction, "createdAt"))
        return chatService.listThreads(principal, pageable)
    }

    @DeleteMapping("/threads/{threadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteThread(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable threadId: UUID,
    ) {
        chatService.deleteThread(principal, threadId)
    }
}
