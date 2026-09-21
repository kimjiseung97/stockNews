package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NewsSearchRequest
import org.kjs.stocknews.model.dto.NewsSearchResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

// 임베딩 서비스의 검색 엔드포인트(POST /v1/search) 클라이언트.
//
// 적재용 NewsEmbeddingClient와 같은 서비스를 부르지만 타임아웃 성격이 정반대라 클라이언트를
// 나눈다. 적재는 배치라 3분을 기다려도 되지만, 검색은 사용자가 화면 앞에서 기다리는 경로다.
// 응답이 늦으면 뉴스 근거를 포기하고 답변을 진행하는 편이 낫다.
@Component
class NewsSearchClient(
    @Value("\${news.vector-search.base-url}") private val baseUrl: String,
    @Value("\${news.vector-search.connect-timeout-ms}") connectTimeoutMs: Long,
    @Value("\${news.vector-search.read-timeout-ms}") readTimeoutMs: Long,
) {
    private val log = LoggerFactory.getLogger(NewsSearchClient::class.java)

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                // HTTP/1.1을 명시한다. JDK HttpClient 기본값은 HTTP/2인데 평문 http로는 ALPN을 못 써서
                // h2c 업그레이드를 시도하고, 임베딩 서비스의 uvicorn(h11)이 그 헤더를 보면 바디를
                // 애플리케이션에 넘기지 않고 보류해 422가 된다(NewsEmbeddingClient에서 실제로 겪었다).
                HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                    .build(),
            ).apply { setReadTimeout(Duration.ofMillis(readTimeoutMs)) },
        )
        .build()

    // 질문과 의미가 가까운 뉴스를 찾는다. 서비스가 준비되지 않았으면(503) null.
    // 그 외 실패는 예외를 그대로 올려 호출 측이 판단하게 둔다.
    fun search(question: String, stockId: Long?, articleLimit: Int): NewsSearchResponse? {
        try {
            return restClient.post()
                .uri("$baseUrl/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(NewsSearchRequest(query = question, stockId = stockId, limit = articleLimit))
                .retrieve()
                .body(NewsSearchResponse::class.java)
        } catch (e: HttpServerErrorException.ServiceUnavailable) {
            // 모델 워밍업 중이거나 벡터 DB 미연결. 장애가 아니라 "지금은 못 준다"라서
            // 뉴스 근거 없이 답변을 이어가면 된다.
            log.info("embedding service not ready (503), answering without news context: {}", e.message)
            return null
        }
    }
}
