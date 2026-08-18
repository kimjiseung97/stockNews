package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.FinnhubCompanyProfile
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Component
class FinnhubCompanyProfileClient(
    @Value("\${finnhub.base-url}") private val baseUrl: String,
    @Value("\${finnhub.api-key}") private val apiKey: String,
) {
    private val log = LoggerFactory.getLogger(FinnhubCompanyProfileClient::class.java)

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(15)) },
        )
        .build()

    // 토스 전용으로 유입돼 영문명이 없는(name=ticker) 종목의 영문 정식 회사명을 조회한다. 못 찾으면 null.
    fun fetchEnglishName(ticker: String): String? {
        val profile = try {
            restClient.get()
                .uri("$baseUrl/api/v1/stock/profile2?symbol=$ticker&token=$apiKey")
                .retrieve()
                .body(FinnhubCompanyProfile::class.java)
        } catch (e: Exception) {
            log.warn("finnhub fetchEnglishName failed for ticker={}", ticker, e)
            null
        }
        return profile?.name?.takeIf { it.isNotBlank() }
    }
}
