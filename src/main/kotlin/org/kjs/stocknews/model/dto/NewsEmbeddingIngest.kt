package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// 임베딩 서비스(embedding-service, FastAPI) POST /v1/news 요청/응답 DTO.
// 파이썬 쪽 스키마가 snake_case라 stockId만 @JsonProperty로 매핑한다.

// 요청 본문 1건. content가 비었거나 너무 길면 서비스가 5xx가 아니라 skipped로 세고 넘어간다.
data class NewsEmbeddingItem(
    val id: Long,
    val title: String,
    val content: String,
    val url: String,
    // 종목 필터 검색(WHERE stock_id = ?)이 벡터 쿼리 안에 들어가야 해서 필수 값이다.
    // 누락하면 서비스가 422로 배치 전체를 거절한다.
    @param:JsonProperty("stock_id")
    @get:JsonProperty("stock_id")
    val stockId: Long,
)

data class NewsEmbeddingRequest(
    val items: List<NewsEmbeddingItem>,
)

// 응답은 요약 카운터일 뿐이고 실제 계약은 상태 코드다(200=배치 전체 완료, 5xx=전체 재전송).
// 로그로만 남긴다.
@JsonIgnoreProperties(ignoreUnknown = true)
data class NewsEmbeddingResponse(
    val news: Int,
    val embedded: Int,
    // 본문 공백/길이 초과로 임베딩 없이 넘어간 건수. 실패가 아니라 정상 종료다.
    val skipped: Int,
    val chunks: Int,
)
