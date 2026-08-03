package org.kjs.stocknews.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("stockNews API")
                .description("미국 주식 뉴스 다이제스트 서비스 API 문서")
                .version("v1"),
        )
}
