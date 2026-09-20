package org.kjs.stocknews.repository

import org.kjs.stocknews.model.dto.NewsChunkHit
import org.kjs.stocknews.vector.repository.NewsChunkJpaRepository
import org.kjs.stocknews.vector.repository.NewsChunkSimilarity
import org.springframework.stereotype.Repository

// 기사 하나가 여러 청크로 쪼개져 저장돼 있다. 그래서 "기사 3건"을 원하면 DB에서는 청크를 그보다
// 넉넉히 꺼내야 한다 - 꺼낸 청크들이 전부 같은 기사일 수도 있기 때문이다. 이 배수가 그 여유분이다.
private const val CHUNKS_FETCHED_PER_ARTICLE = 5

// 벡터 조회의 입출력을 도메인 쪽 표현으로 맞춰주는 얇은 층.
//
// 실제 SQL은 NewsChunkJpaRepository의 네이티브 쿼리가 들고 있고, 여기서는 그 쿼리가 요구하는
// 형태로 값을 바꿔 넘기고(FloatArray -> pgvector 리터럴, 유사도 -> 거리) 결과를 기사 단위로 접는다.
//
// 이 파일에는 성격이 다른 두 종류의 목록이 나온다. 헷갈리면 결과 개수가 조용히 틀어지므로
// 이름으로 구분한다.
//   matchedChunks - DB가 돌려준 청크 행 목록. 같은 기사가 여러 번 들어 있을 수 있다.
//   articles      - 기사당 하나만 남기고 접은 뒤의 목록. 호출 측이 실제로 쓰는 결과다.
@Repository
class NewsChunkRepository(
    private val newsChunkJpaRepository: NewsChunkJpaRepository,
) {
    /**
     * 질의 벡터와 가까운 뉴스를 찾는다. stockId가 null이면 종목 필터 없이 전체에서 찾는다.
     *
     * articleLimit은 청크가 아니라 **기사** 개수다.
     */
    fun findSimilar(
        queryVector: FloatArray,
        stockId: Long?,
        articleLimit: Int,
        minScore: Double,
    ): List<NewsChunkHit> {
        val vectorLiteral = toVectorLiteral(queryVector)
        // 쿼리는 유사도가 아니라 거리로 비교한다. 거리 = 1 - 유사도라 부등호 방향이 뒤집힌다.
        val maxDistance = 1.0 - minScore
        val chunkLimit = articleLimit * CHUNKS_FETCHED_PER_ARTICLE

        // 종목 조건 유무에 따라 쿼리가 나뉘어 있다. 한 벌로 합치면 플래너가 stock_id 인덱스를
        // 쓰는 계획을 고를 수 없다(NewsChunkJpaRepository 주석 참고).
        val matchedChunks = if (stockId == null) {
            newsChunkJpaRepository.findSimilarAcrossStocks(vectorLiteral, maxDistance, chunkLimit)
        } else {
            newsChunkJpaRepository.findSimilarByStock(vectorLiteral, stockId, maxDistance, chunkLimit)
        }

        return foldChunksIntoArticles(matchedChunks.map { it.toHit() }, articleLimit)
    }

    // 기사당 가장 가까운 청크 하나만 남긴다. 입력이 거리순이라 첫 등장이 곧 그 기사의 최선이다.
    // 접지 않으면 같은 기사의 청크들이 결과를 전부 차지한다.
    private fun foldChunksIntoArticles(
        matchedChunks: List<NewsChunkHit>,
        articleLimit: Int,
    ): List<NewsChunkHit> {
        val articles = mutableListOf<NewsChunkHit>()
        val seenNewsIds = mutableSetOf<Long>()
        for (chunk in matchedChunks) {
            if (!seenNewsIds.add(chunk.newsId)) {
                continue
            }
            articles.add(chunk)
            if (articles.size == articleLimit) {
                break
            }
        }
        return articles
    }

    private fun NewsChunkSimilarity.toHit(): NewsChunkHit =
        NewsChunkHit(
            newsId = getNewsId(),
            title = getTitle(),
            content = getContent(),
            url = getUrl(),
            score = getScore(),
        )

    // pgvector는 '[0.1,0.2,...]' 형태의 문자열을 vector로 캐스팅해 받는다.
    private fun toVectorLiteral(vector: FloatArray): String =
        vector.joinToString(prefix = "[", postfix = "]", separator = ",")
}
