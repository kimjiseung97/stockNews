package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NaverNewsSearchResponse
import org.kjs.stocknews.model.dto.NewsArticle
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private data class CachedNaverNews(val articles: List<NewsArticle>, val fetchedAt: Instant)

private val HTML_TAG_REGEX = Regex("<.*?>")

@Component
class NaverNewsClient(
    @Value("\${naver.news.client-id}") private val clientId: String,
    @Value("\${naver.news.client-secret}") private val clientSecret: String,
    @Value("\${naver.news.max-articles-per-query}") private val maxArticlesPerQuery: Int,
    @Value("\${naver.news.cache-ttl-minutes}") private val cacheTtlMinutes: Long,
) {
    private val log = LoggerFactory.getLogger(NaverNewsClient::class.java)

    // 검색어별 조회 결과를 TTL 동안 전역 캐시해서, 같은 종목을 구독한 여러 유저를 처리할 때 API 호출을 한 번으로 줄인다.
    private val cache = ConcurrentHashMap<String, CachedNaverNews>()

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(15)) },
        )
        .build()

    fun fetchNews(query: String): List<NewsArticle> {
        val cached = cache[query]
        if (cached != null && cached.fetchedAt.plusSeconds(cacheTtlMinutes * 60).isAfter(Instant.now())) {
            return cached.articles
        }

        val articles = fetchFromApi(query)
        cache[query] = CachedNaverNews(articles, Instant.now())
        return articles
    }

    private fun fetchFromApi(query: String): List<NewsArticle> {
        val response = try {
            restClient.get()
                .uri { builder ->
                    builder
                        .scheme("https")
                        .host("openapi.naver.com")
                        .path("/v1/search/news.json")
                        .queryParam("query", query)
                        .queryParam("display", maxArticlesPerQuery)
                        .queryParam("sort", "sim")
                        .build()
                }
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .body(NaverNewsSearchResponse::class.java)
        } catch (e: Exception) {
            log.warn("naver fetchNews failed for query={}", query, e)
            null
        }

        return response?.items
            ?.mapNotNull { item ->
                val link = item.link ?: return@mapNotNull null
                NewsArticle(title = unescapeHtml(item.title.orEmpty()), url = link)
            }
            ?: emptyList()
    }

    private fun unescapeHtml(text: String): String =
        text.replace(HTML_TAG_REGEX, "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
}
