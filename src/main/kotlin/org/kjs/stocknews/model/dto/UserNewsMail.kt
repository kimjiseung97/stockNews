package org.kjs.stocknews.model.dto

data class UserNewsMail(
    val email: String,
    val articlesByTicker: Map<String, List<NewsArticle>>,
)
