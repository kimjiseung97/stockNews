package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SecTickerEntry(
    @JsonProperty("cik_str")
    val cikStr: Long,
    val ticker: String,
    val title: String,
)
