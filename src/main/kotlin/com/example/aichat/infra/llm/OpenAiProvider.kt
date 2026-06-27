package com.example.aichat.infra.llm

import com.example.aichat.common.config.AppProperties
import com.example.aichat.common.exception.ApiException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Flux
import java.time.Duration

/**
 * OpenAI Chat Completions API 연동.
 * api-key 가 설정된 경우에만 LlmConfig 가 이 빈을 활성화한다.
 */
class OpenAiProvider(
    private val config: AppProperties.OpenAi,
    webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper,
) : LlmProvider {

    override val name: String = "openai"

    private val requestTimeout: Duration = Duration.ofSeconds(60)

    private val webClient: WebClient = webClientBuilder
        .baseUrl(config.baseUrl)
        .defaultHeader("Authorization", "Bearer ${config.apiKey}")
        .build()

    private fun resolveModel(model: String?): String =
        model?.takeIf { it.isNotBlank() } ?: config.defaultModel

    private fun requestBody(messages: List<LlmMessage>, model: String, stream: Boolean): Map<String, Any> =
        mapOf(
            "model" to model,
            "stream" to stream,
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
        )

    override fun chat(messages: List<LlmMessage>, model: String?): LlmResult {
        val resolved = resolveModel(model)
        val response = try {
            webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody(messages, resolved, stream = false))
                .retrieve()
                .bodyToMono<String>()
                .block(requestTimeout)
        } catch (e: WebClientResponseException) {
            throw ApiException(HttpStatus.BAD_GATEWAY, "OpenAI 호출에 실패했습니다 (${e.statusCode.value()}).")
        } ?: throw ApiException(HttpStatus.BAD_GATEWAY, "OpenAI 응답이 비어 있습니다.")

        val root = objectMapper.readTree(response)
        val content = root.path("choices").firstOrNull()
            ?.path("message")?.path("content")?.asText()
            ?: throw ApiException(HttpStatus.BAD_GATEWAY, "OpenAI 응답 파싱에 실패했습니다.")
        val usedModel = root.path("model").asText(resolved)
        return LlmResult(content = content, model = usedModel)
    }

    override fun streamChat(messages: List<LlmMessage>, model: String?): Flux<String> {
        val resolved = resolveModel(model)
        return webClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(requestBody(messages, resolved, stream = true))
            .retrieve()
            .bodyToFlux(String::class.java)
            .timeout(requestTimeout)
            .takeUntil { it.trim() == "[DONE]" }
            .filter { it.trim() != "[DONE]" && it.isNotBlank() }
            .map { chunk -> parseDelta(chunk) }
            .filter { it.isNotEmpty() }
    }

    /** SSE data 페이로드(JSON)에서 choices[0].delta.content 를 추출한다. */
    private fun parseDelta(chunk: String): String {
        return runCatching {
            val node: JsonNode = objectMapper.readTree(chunk)
            node.path("choices").firstOrNull()
                ?.path("delta")?.path("content")?.asText("") ?: ""
        }.getOrDefault("")
    }
}
