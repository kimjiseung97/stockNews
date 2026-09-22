package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// OpenAI Chat Completions 규격 요청. NVIDIA NIM(NvidiaChatClient)과 OpenAI(GptClient)가 같이 쓴다.
//
// null인 필드는 직렬화에서 빠진다. 제공자/모델마다 받는 파라미터가 달라서다:
//  - NVIDIA NIM: max_tokens + temperature + top_p, reasoning_effort는 지원 모델에만
//  - OpenAI GPT-5 계열: max_tokens 대신 max_completion_tokens, temperature/top_p는 기본값만 허용(보내면 400)
// `"reasoning_effort": null`처럼 키를 남겨 보내면 거절하는 서버가 있어 키 자체가 없어야 한다.
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OpenAiChatCompletionRequest(
    val model: String,
    val messages: List<OpenAiChatMessage>,
    val temperature: Double? = null,
    @JsonProperty("top_p")
    val topP: Double? = null,
    @JsonProperty("max_tokens")
    val maxTokens: Int? = null,
    @JsonProperty("max_completion_tokens")
    val maxCompletionTokens: Int? = null,
    // 답을 내기 전에 태우는 추론 분량. 지원하지 않는 모델에는 null로 두어 보내지 않는다.
    @JsonProperty("reasoning_effort")
    val reasoningEffort: String? = null,
    val stream: Boolean = false,
)
