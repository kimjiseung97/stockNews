package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class NvidiaChatCompletionResponse(
    val choices: List<NvidiaChatChoice>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NvidiaChatChoice(
    val message: NvidiaChatMessage?,
    // "stop"이면 정상 종료, "length"면 max_tokens에 잘린 것. content가 비어 왔을 때 원인을 가르는 단서라
    // 실패 메시지에 실어 로그로 남긴다.
    @JsonProperty("finish_reason")
    val finishReason: String? = null,
)
