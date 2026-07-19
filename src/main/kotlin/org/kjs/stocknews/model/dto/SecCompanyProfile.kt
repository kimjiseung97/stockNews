package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class SecCompanyProfile(
    val sic: String?,
    val sicDescription: String?,
)
