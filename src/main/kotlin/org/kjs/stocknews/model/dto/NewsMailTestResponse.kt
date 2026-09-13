package org.kjs.stocknews.model.dto

// 테스트 발송 결과 요약. 실제로 어디로 몇 건 나갔는지 바로 확인할 수 있게 내려준다.
data class NewsMailTestResponse(
    val email: String,
    val stockCount: Int,
    val articleCount: Int,
)
