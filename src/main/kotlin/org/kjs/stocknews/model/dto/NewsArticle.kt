package org.kjs.stocknews.model.dto

data class NewsArticle(
    val title: String,
    val url: String,
    val description: String? = null,
)
