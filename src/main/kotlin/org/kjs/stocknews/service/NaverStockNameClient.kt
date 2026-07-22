package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NaverStockSearchResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Component
class NaverStockNameClient {
    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(15)) },
        )
        .build()

    fun fetchKoreanName(ticker: String): String? {
        val name = search(ticker)
        if (name != null) return name

        // Naver uses "." for share-class tickers (e.g. BRK.B) where our data uses "-" (e.g. BRK-B).
        if (ticker.contains('-')) {
            return search(ticker.replace('-', '.'))
        }
        return null
    }

    private fun search(ticker: String): String? {
        val response = restClient.get()
            .uri { builder ->
                builder
                    .scheme("https")
                    .host("ac.stock.naver.com")
                    .path("/ac")
                    .queryParam("q", ticker)
                    .queryParam("target", "stock,itemNo,marketindex,coinExchange,industry")
                    .build()
            }
            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .retrieve()
            .body(NaverStockSearchResponse::class.java)

        return response?.items
            ?.firstOrNull { it.category == "stock" && it.code.equals(ticker, ignoreCase = true) }
            ?.name
    }
}
