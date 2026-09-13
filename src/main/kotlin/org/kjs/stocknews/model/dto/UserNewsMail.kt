package org.kjs.stocknews.model.dto

import java.time.LocalTime

// 발송 대상 유저 1명분의 다이제스트 메일.
// userId/dispatchTime은 메일 본문에는 쓰이지 않지만 발송 결과를 TB_MAIL_DISPATCH_LOG에 남길 때 필요하다.
data class UserNewsMail(
    val userId: Long,
    val email: String,
    val dispatchTime: LocalTime,
    val articlesByTicker: Map<String, List<NewsArticle>>,
) {
    val stockCount: Int
        get() = articlesByTicker.size

    val articleCount: Int
        get() = articlesByTicker.values.sumOf { it.size }
}
