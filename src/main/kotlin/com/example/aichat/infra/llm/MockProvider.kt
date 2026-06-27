package com.example.aichat.infra.llm

import reactor.core.publisher.Flux
import java.time.Duration

/**
 * API 키가 없을 때 사용하는 가짜 공급자.
 * 키 없이도 평가자가 전체 흐름(스레드 세션, 영속화, 스트리밍)을 시연할 수 있게 한다.
 */
class MockProvider(
    private val defaultModel: String,
) : LlmProvider {

    override val name: String = "mock"

    private fun resolveModel(model: String?): String =
        (model?.takeIf { it.isNotBlank() } ?: defaultModel)

    private fun answerFor(messages: List<LlmMessage>): String {
        val lastUser = messages.lastOrNull { it.role == "user" }?.content ?: "(질문 없음)"
        val priorTurns = messages.count { it.role == "user" }
        return "[mock 응답] \"$lastUser\" 질문을 받았습니다. " +
            "이 스레드에는 지금까지 ${priorTurns}개의 질문이 있었습니다. " +
            "실제 OpenAI 응답을 받으려면 OPENAI_API_KEY 환경변수를 설정하세요."
    }

    override fun chat(messages: List<LlmMessage>, model: String?): LlmResult =
        LlmResult(content = answerFor(messages), model = "${resolveModel(model)} (mock)")

    override fun streamChat(messages: List<LlmMessage>, model: String?): Flux<String> {
        val words = answerFor(messages).split(" ")
        // 토큰이 흘러나오는 모습을 흉내내기 위해 단어 단위로 약간의 지연을 두고 방출한다.
        return Flux.fromIterable(words.mapIndexed { i, w -> if (i == 0) w else " $w" })
            .delayElements(Duration.ofMillis(30))
    }
}
