package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.TossListedStock
import org.kjs.stocknews.model.dto.TossListedStockResponse
import org.kjs.stocknews.model.dto.TossTokenResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.time.Instant

// 토스증권이 미국주식으로 취급하는 3개 거래소. securityType=STOCK, commonShare=true로 우선주/워런트/ETF 등은 제외하고 보통주만 조회.
private val US_MARKETS = listOf("NASDAQ", "NYSE", "AMEX")

// 만료 직전에 재사용하다 401을 맞지 않도록 만료시각보다 이만큼 일찍 갱신한다.
private val TOKEN_EXPIRY_MARGIN = Duration.ofMinutes(1)

// 토스증권 공식 문서상 종목 마스터 데이터는 내부적으로 하루 1회 갱신 - 그보다 자주(예: stockSeedJob의 2분 주기) 조회할
// 필요가 없어 로컬 캐싱한다. 데이터 규모(3개 거래소 합쳐 최대 1만 건 내외)가 작아 Redis 등 별도 인프라 없이 인메모리로 충분.
private val LISTING_CACHE_TTL = Duration.ofHours(24)

private data class CachedListing(val stocks: List<TossListedStock>, val fetchedAt: Instant)

@Component
class TossStockClient(
    private val environment: Environment,
    @Value("\${toss.base-url:https://openapi.tossinvest.com}") private val baseUrl: String,
    @Value("\${toss.client-id:}") private val clientId: String,
    @Value("\${toss.client-secret:}") private val clientSecret: String,
) {
    private val log = LoggerFactory.getLogger(TossStockClient::class.java)

    private val restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(30)) },
        )
        .build()

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var tokenExpiresAt: Instant = Instant.MIN

    @Volatile
    private var cachedListing: CachedListing? = null

    // local 프로필로 기동됐는지는 spring.profiles.active를 직접 확인한다 - 크레덴셜 빈 값 여부로 추측하면
    // prod에서 환경변수가 실수로 빠졌을 때도 조용히 스킵돼버려서 설정 실수를 못 알아챈다.
    private val localProfile = environment.activeProfiles.contains("local")

    // 앱 기동 완료 직후 한 번 미리 적재해둔다 - 첫 stockSeedJob 실행 때 토스 API 호출을 기다리지 않도록.
    // 실패해도 기동 자체를 막지 않고 로그만 남긴다(다음 fetchAllUsListed 호출 때 재시도).
    @EventListener(ApplicationReadyEvent::class)
    fun preloadOnStartup() {
        try {
            fetchAllUsListed()
        } catch (e: Exception) {
            log.warn("toss listing preload on startup failed, will retry on first use", e)
        }
    }

    // 나스닥/뉴욕/아멕스 3개 거래소의 보통주 목록을 합쳐서 반환한다(심볼 기준 중복 제거).
    // 하루 1회만 바뀌는 데이터라 TTL 동안은 캐시를 그대로 재사용하고 API를 호출하지 않는다.
    @Synchronized
    fun fetchAllUsListed(): List<TossListedStock> {
        if (localProfile) {
            log.info("local profile active, skipping toss stock lookup")
            return emptyList()
        }

        cachedListing?.let { cached ->
            if (cached.fetchedAt.plus(LISTING_CACHE_TTL).isAfter(Instant.now())) {
                return cached.stocks
            }
        }

        val merged = US_MARKETS.flatMap { market -> fetchListed(market) }.distinctBy { it.symbol }
        log.info("toss fetched {} us common stocks across {}", merged.size, US_MARKETS)
        cachedListing = CachedListing(merged, Instant.now())
        return merged
    }

    private fun fetchListed(market: String): List<TossListedStock> {
        val response = restClient.get()
            .uri { builder ->
                builder
                    .path("/api/v1/stocks/all")
                    .queryParam("market", market)
                    .queryParam("securityType", "STOCK")
                    .queryParam("commonShare", true)
                    .build()
            }
            .header("Authorization", "Bearer ${accessToken()}")
            .retrieve()
            .body(TossListedStockResponse::class.java)
        return response?.result ?: emptyList()
    }

    @Synchronized
    private fun accessToken(): String {
        cachedToken?.let { token ->
            if (Instant.now().isBefore(tokenExpiresAt)) return token
        }

        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
            add("client_id", clientId)
            add("client_secret", clientSecret)
        }
        val response = restClient.post()
            .uri("/oauth2/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(TossTokenResponse::class.java)
            ?: error("failed to obtain toss access token: empty response")

        cachedToken = response.accessToken
        tokenExpiresAt = Instant.now().plusSeconds(response.expiresInSeconds).minus(TOKEN_EXPIRY_MARGIN)
        log.info("toss access token refreshed, expires in {}s", response.expiresInSeconds)
        return response.accessToken
    }
}
