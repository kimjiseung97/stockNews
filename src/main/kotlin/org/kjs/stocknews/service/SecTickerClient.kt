package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.SecTickerEntry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Component
class SecTickerClient(
    @Value("\${sec.tickers-url}") private val tickersUrl: String,
    @Value("\${sec.user-agent}") private val userAgent: String,
) {
    private val log = LoggerFactory.getLogger(SecTickerClient::class.java)

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(30)) },
        )
        .build()

    fun fetchAllTickers(): List<SecTickerEntry> {
        val t0 = System.currentTimeMillis()
        val result = restClient.get()
            .uri(tickersUrl)
            .header(HttpHeaders.USER_AGENT, userAgent)
            .retrieve()
            .body(object : ParameterizedTypeReference<Map<String, SecTickerEntry>>() {})
            ?.values
            ?.toList()
            ?: emptyList()
        log.info("sec.gov fetched {} tickers in {} ms", result.size, System.currentTimeMillis() - t0)
        return result
    }
}
