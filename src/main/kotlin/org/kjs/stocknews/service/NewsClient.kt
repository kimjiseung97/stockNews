package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NewsArticle

interface NewsClient {
    fun fetchNews(ticker: String): List<NewsArticle>
}
