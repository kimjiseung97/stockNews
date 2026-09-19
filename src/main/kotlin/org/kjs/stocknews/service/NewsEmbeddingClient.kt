package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NewsEmbeddingItem
import org.kjs.stocknews.model.dto.NewsEmbeddingRequest
import org.kjs.stocknews.model.dto.NewsEmbeddingResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

// 사내(컨테이너 내부) 임베딩 서비스 클라이언트. 배포에서는 stocknews_internal 망의
// http://embedding:8000 으로 붙는다(외부에 공개되지 않은 무인증 API라 내부망 밖에서 부르지 말 것).
//
// 계약은 상태 코드다.
// - 200: 배치 전체 처리 완료. 보낸 건을 모두 완료 처리하면 된다.
// - 503: 서비스가 아직 안 떴거나 벡터 DB 미연결. 장애가 아니라 "나중에 다시"라서 null을 돌려준다.
// - 그 외 4xx/5xx: 예외를 그대로 올려 호출 측(배치)이 실패로 끝나게 한다. 다음 주기에 같은 배치를
//   다시 보내면 되고, 서비스가 upsert라 중복 적재되지 않는다.
@Component
class NewsEmbeddingClient(
    @Value("\${news.embedding.base-url}") private val baseUrl: String,
    @Value("\${news.embedding.connect-timeout-ms}") connectTimeoutMs: Long,
    // 임베딩은 CPU 추론이라 한 배치가 수십 초 걸릴 수 있다. 읽기 타임아웃을 짧게 잡으면
    // 서비스는 정상 처리했는데 호출 측만 실패로 보고 같은 배치를 계속 재전송하게 된다.
    @Value("\${news.embedding.read-timeout-ms}") readTimeoutMs: Long,
) {
    private val log = LoggerFactory.getLogger(NewsEmbeddingClient::class.java)

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                // HTTP/1.1을 명시한다. JDK HttpClient 기본값은 HTTP/2인데, 평문 http로는 ALPN을 쓸 수 없어
                // `Connection: Upgrade` + `Upgrade: h2c` 헤더를 붙인 h2c 업그레이드를 시도한다.
                // 임베딩 서비스의 uvicorn(h11)은 h2c를 모르면서도 그 헤더를 보면 프로토콜 전환 가능성 때문에
                // 바디를 애플리케이션에 넘기지 않고 보류해버린다. 결과적으로 FastAPI에는 빈 바디가 도착해
                // 422(loc=["body"], "Field required")가 돌아오고, 보류된 바디는 다음 요청으로 재파싱되며 깨진다.
                HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                    .build(),
            ).apply { setReadTimeout(Duration.ofMillis(readTimeoutMs)) },
        )
        .build()

    // 뉴스 묶음을 임베딩 서비스에 넘긴다. 성공하면 처리 요약, 서비스가 아직 준비되지 않았으면 null.
    fun ingest(items: List<NewsEmbeddingItem>): NewsEmbeddingResponse? {
        try {
            return restClient.post()
                .uri("$baseUrl/v1/news")
                .contentType(MediaType.APPLICATION_JSON)
                .body(NewsEmbeddingRequest(items))
                .retrieve()
                .body(NewsEmbeddingResponse::class.java)
        } catch (e: HttpServerErrorException.ServiceUnavailable) {
            log.info("embedding service not ready (503), retrying next run: {}", e.message)
            return null
        }
    }
}
