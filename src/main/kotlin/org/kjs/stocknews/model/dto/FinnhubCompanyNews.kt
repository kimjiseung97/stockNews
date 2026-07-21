package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FinnhubCompanyNews(
    val headline: String,
    val url: String,
    val datetime: Long,
)
