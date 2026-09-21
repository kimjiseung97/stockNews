package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// 임베딩 서비스(POST /v1/search)에 보내는 검색 요청.
//
// 질문 원문만 넘긴다 - 프리픽스("query: ")를 붙이고 임베딩하고 pgvector를 뒤지는 일은
// 전부 저쪽이 한다. 이 앱은 모델도 벡터 DB도 모른다.
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NewsSearchRequest(
    val query: String,
    // null이면 종목을 가리지 않고 전체에서 찾는다 - 종목명을 안 쓴 질문("반도체 업황 어때?")도
    // 근거를 받을 수 있어야 하기 때문이다.
    @JsonProperty("stock_id")
    val stockId: Long?,
    // 청크가 아니라 기사 개수다. 프롬프트에 몇 건을 넣을지는 토큰 비용과 응답 지연에 직결되므로
    // 이 앱이 정해서 보낸다.
    val limit: Int,
)
