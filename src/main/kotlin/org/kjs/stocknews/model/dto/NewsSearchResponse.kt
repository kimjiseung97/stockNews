package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// 임베딩 서비스(POST /v1/search) 응답. 이미 기사 단위로 접힌 결과가 온다.
@JsonIgnoreProperties(ignoreUnknown = true)
data class NewsSearchResponse(
    val articles: List<NewsSearchArticle>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NewsSearchArticle(
    @JsonProperty("news_id")
    val newsId: Long,
    val title: String,
    val content: String,
    val url: String,
    // 1 - 코사인 거리. 값이 절대적 관련성을 뜻하지는 않는다(임계값으로 쓰지 말 것 -
    // 무관한 질문도 관련 질문과 비슷한 점수가 나온다. 판단은 임베딩 서비스가 한다).
    val score: Double,
)
