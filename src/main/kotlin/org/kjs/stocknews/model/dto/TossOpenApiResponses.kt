package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresInSeconds: Long,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossListedStock(
    val symbol: String,
    // 토스증권 응답의 name은 미국 종목도 한글명으로 온다(예: AAPL -> "애플") - 영문명 아님, koreanName 매핑 전용.
    val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossListedStockResponse(
    val result: List<TossListedStock>,
)
