package org.kjs.stocknews.model.dto

// pgvector(news_chunks)에서 꺼낸 검색 결과 1건. 기사 단위로 접힌 뒤의 값이다.
//
// content는 기사 전문이 아니라 질문과 가장 가까웠던 청크다. 파이썬 embedding-service가
// 본문을 청크로 쪼개 적재하고, 임베딩에는 "제목\n청크"를 쓰되 저장은 제목을 뺀 청크만 한다.
data class NewsChunkHit(
    val newsId: Long,
    val title: String,
    val content: String,
    val url: String,
    // 1 - 코사인 거리. 1에 가까울수록 질문과 가깝다.
    val score: Double,
)
