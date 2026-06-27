package com.example.aichat.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 애플리케이션 전역 설정. application.yml 의 `app.*` 에 매핑된다.
 */
@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: Jwt,
    val seed: Seed,
    val llm: Llm,
) {
    data class Jwt(
        val secret: String,
        val expirationMillis: Long,
    )

    data class Seed(
        val enabled: Boolean,
    )

    data class Llm(
        val openai: OpenAi,
    )

    data class OpenAi(
        val apiKey: String,
        val baseUrl: String,
        val defaultModel: String,
    )
}
