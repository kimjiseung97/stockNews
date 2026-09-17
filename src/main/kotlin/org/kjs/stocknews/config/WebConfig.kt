package org.kjs.stocknews.config

import org.kjs.stocknews.config.ratelimit.RateLimitInterceptor
import org.kjs.stocknews.config.ratelimit.RateLimitProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver
import java.util.concurrent.TimeUnit

@ConfigurationProperties(prefix = "cors")
class CorsProperties {
    var allowedOrigins: List<String> = emptyList()
}

@Configuration
@EnableConfigurationProperties(CorsProperties::class, RateLimitProperties::class)
class WebConfig(
    private val corsProperties: CorsProperties,
    private val authInterceptor: AuthInterceptor,
    private val rateLimitInterceptor: RateLimitInterceptor,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*corsProperties.allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 요청 제한을 인증보다 먼저 태운다 - 비로그인 상태로 쏟아지는 요청까지 막아야 하고,
        // 세션 조회보다 버킷 차감이 싸다.
        // 대상은 API 경로만 나열한다. 이 프로젝트는 API에 /api 프리픽스가 없고 SPA 정적 리소스가
        // /** 로 서빙되므로, /** 로 걸면 js/css/이미지 한 번 받는 것까지 요청 수로 세게 된다.
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns(
                "/auth/**",
                "/stocks/**",
                "/users/me/**",
            )
            .order(0)

        registry.addInterceptor(authInterceptor)
            .addPathPatterns(
                "/users/me/**",
                // 프론트는 비로그인 시 챗봇 입력창을 비활성화하지만 그건 화면 가드일 뿐이라,
                // curl로 직접 부르면 그대로 통과해 NVIDIA 토큰 비용이 나갔다. 서버에서도 막는다.
                "/stocks/chat",
                "/watchlist",
                "/watchlist/register",
                "/email-settings",
            )
            .order(1)
    }

    // Vite가 해시 파일명으로 만드는 정적 자산은 영구 캐시, 나머지(=SPA 라우트)는 index.html로 폴백
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())

        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.noCache())
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource {
                    val requestedResource = location.createRelative(resourcePath)
                    return if (requestedResource.exists() && requestedResource.isReadable) {
                        requestedResource
                    } else {
                        ClassPathResource("/static/index.html")
                    }
                }
            })
    }
}
