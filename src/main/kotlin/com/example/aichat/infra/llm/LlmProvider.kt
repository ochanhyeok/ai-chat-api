package com.example.aichat.infra.llm

import reactor.core.publisher.Flux

/** LLM 에 전달하는 단일 메시지. role 은 "system" | "user" | "assistant". */
data class LlmMessage(
    val role: String,
    val content: String,
)

/** 동기 응답 결과. 실제 사용된 모델명을 함께 돌려준다. */
data class LlmResult(
    val content: String,
    val model: String,
)

/**
 * LLM 공급자 추상화.
 *
 * 이 인터페이스가 이 과제의 확장 지점이다.
 * - OpenAI 외 다른 provider(Anthropic 등)를 추가하거나
 * - 사내 대외비 문서를 검색해 컨텍스트로 주입하는 RAG provider 로 교체하더라도
 * 상위 ChatService / 외부 API 계약은 바뀌지 않는다.
 */
interface LlmProvider {
    /** 공급자 식별용 이름 (로깅/디버깅). */
    val name: String

    /** 동기 응답 생성. */
    fun chat(messages: List<LlmMessage>, model: String?): LlmResult

    /** 스트리밍 응답 생성. 각 원소는 응답의 부분 토큰(delta)이다. */
    fun streamChat(messages: List<LlmMessage>, model: String?): Flux<String>
}
