package com.example.aichat.infra.llm

import com.example.aichat.common.config.AppProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * 활성 LlmProvider 를 결정한다.
 * OPENAI_API_KEY 가 주어지면 OpenAiProvider, 아니면 MockProvider 로 자동 폴백한다.
 */
@Configuration
class LlmConfig {

    private val log = LoggerFactory.getLogger(LlmConfig::class.java)

    @Bean
    fun llmProvider(
        props: AppProperties,
        webClientBuilder: WebClient.Builder,
        objectMapper: ObjectMapper,
    ): LlmProvider {
        val openai = props.llm.openai
        return if (openai.apiKey.isNotBlank()) {
            log.info("LLM provider: OpenAI (model={})", openai.defaultModel)
            OpenAiProvider(openai, webClientBuilder, objectMapper)
        } else {
            log.warn("OPENAI_API_KEY 미설정 → MockProvider 사용 (데모 모드)")
            MockProvider(openai.defaultModel)
        }
    }
}
