package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NvidiaChatCompletionRequest(
    val model: String,
    val messages: List<NvidiaChatMessage>,
    val temperature: Double,
    @JsonProperty("top_p")
    val topP: Double,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
    val stream: Boolean = false,
)
