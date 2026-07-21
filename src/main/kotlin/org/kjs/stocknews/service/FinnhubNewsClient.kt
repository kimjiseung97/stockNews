package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.FinnhubCompanyNews
import org.kjs.stocknews.model.dto.NewsArticle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

private data class CachedNews(val articles: List<NewsArticle>, val fetchedAt: Instant)

@Component
class FinnhubNewsClient(
    @Value("\${finnhub.base-url}") private val baseUrl: String,
    @Value("\${finnhub.api-key}") private val apiKey: String,
    @Value("\${finnhub.lookback-days}") private val lookbackDays: Long,
    @Value("\${finnhub.cache-ttl-minutes}") private val cacheTtlMinutes: Long,
    @Value("\${finnhub.max-articles-per-ticker}") private val maxArticlesPerTicker: Int,
) : NewsClient {
    private val log = LoggerFactory.getLogger(FinnhubNewsClient::class.java)

    // 티커별 조회 결과를 TTL 동안 전역 캐시해서, 같은 티커를 구독한 여러 유저를 처리할 때 API 호출을 한 번으로 줄인다.
    private val cache = ConcurrentHashMap<String, CachedNews>()

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(15)) },
        )
        .build()

    override fun fetchNews(ticker: String): List<NewsArticle> {
        val cached = cache[ticker]
        if (cached != null && cached.fetchedAt.plusSeconds(cacheTtlMinutes * 60).isAfter(Instant.now())) {
            return cached.articles
        }

        val articles = fetchFromApi(ticker)
        cache[ticker] = CachedNews(articles, Instant.now())
        return articles
    }

    private fun fetchFromApi(ticker: String): List<NewsArticle> {
        val to = LocalDate.now()
        val from = to.minusDays(lookbackDays)

        val response = try {
            restClient.get()
                .uri("$baseUrl/api/v1/company-news?symbol=$ticker&from=$from&to=$to&token=$apiKey")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<FinnhubCompanyNews>>() {})
        } catch (e: Exception) {
            log.warn("finnhub fetchNews failed for ticker={}", ticker, e)
            null
        }

        return response
            ?.sortedByDescending { it.datetime }
            ?.take(maxArticlesPerTicker)
            ?.map { NewsArticle(title = it.headline, url = it.url) }
            ?: emptyList()
    }
}
