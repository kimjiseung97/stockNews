package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// null인 필드는 직렬화에서 빠진다. reasoningEffort를 안 보내는 경우 키 자체가 없어야
// 모델이 기본값으로 동작한다(`"reasoning_effort": null`을 보내면 거절하는 서버가 있다).
@JsonInclude(JsonInclude.Include.NON_NULL)
data class NvidiaChatCompletionRequest(
    val model: String,
    val messages: List<NvidiaChatMessage>,
    val temperature: Double,
    @JsonProperty("top_p")
    val topP: Double,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
    // 답을 내기 전에 태우는 추론 토큰의 분량(low/medium/high). gpt-oss 계열만 이해하는 파라미터라
    // 지원하지 않는 모델에는 null로 두어 보내지 않는다. NvidiaChatClient가 모델별로 정한다.
    @JsonProperty("reasoning_effort")
    val reasoningEffort: String? = null,
    val stream: Boolean = false,
)
