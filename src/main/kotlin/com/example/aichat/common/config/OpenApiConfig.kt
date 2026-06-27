package com.example.aichat.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger UI(/swagger-ui.html) 에서 바로 API 를 탐색·시연할 수 있게 한다.
 * 상단 Authorize 버튼에 로그인으로 받은 JWT 를 넣으면 보호된 엔드포인트도 호출된다.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val schemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("AI Chat API")
                    .version("v1")
                    .description(
                        "LLM(OpenAI 등)을 인증·대화기록·피드백·분석이 붙은 자체 API 로 감싼 챗봇 서버.\n\n" +
                            "사용법: 1) /api/auth/login 으로 토큰 발급 → 2) 우측 상단 Authorize 에 토큰 입력 → " +
                            "3) 보호된 API 호출. (데모 계정: member@demo.com / member1234, admin@demo.com / admin1234)",
                    ),
            )
            .addSecurityItem(SecurityRequirement().addList(schemeName))
            .components(
                Components().addSecuritySchemes(
                    schemeName,
                    SecurityScheme()
                        .name(schemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
    }
}
