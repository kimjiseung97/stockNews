package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NewsArticle
import org.springframework.stereotype.Component

@Component
class StubNewsClient : NewsClient {
    override fun fetchNews(ticker: String): List<NewsArticle> = emptyList()
}
