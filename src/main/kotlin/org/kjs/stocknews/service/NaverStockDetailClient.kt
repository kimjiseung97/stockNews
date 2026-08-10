package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NaverStockOverviewResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Component
class NaverStockDetailClient {
    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(15)) },
        )
        .build()

    fun fetchOverview(reutersCode: String): NaverStockOverviewResponse? =
        restClient.get()
            .uri("https://api.stock.naver.com/stock/{reutersCode}/overview", reutersCode)
            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .retrieve()
            .body(NaverStockOverviewResponse::class.java)
}
