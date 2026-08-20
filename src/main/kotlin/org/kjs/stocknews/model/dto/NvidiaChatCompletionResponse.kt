package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class NvidiaChatCompletionResponse(
    val choices: List<NvidiaChatChoice>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NvidiaChatChoice(
    val message: NvidiaChatMessage?,
)
