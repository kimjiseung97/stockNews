package org.kjs.stocknews.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.kjs.stocknews.vector.repository.NewsChunkJpaRepository
import org.kjs.stocknews.vector.repository.NewsChunkSimilarity
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

private fun <T> anyArg(): T = ArgumentMatchers.any()

// 네이티브 쿼리 자체는 실제 Postgres가 있어야 결과를 볼 수 있다(그건 통합 테스트 영역이다).
// 여기서 막고 싶은 것은 그 쿼리에 무엇을 넘기고 결과를 어떻게 접는지다 -
// 유사도/거리 변환이 뒤집히거나 후보를 덜 뽑는 실수는 에러 없이 결과만 틀어놓는다.
class NewsChunkRepositoryTest {
    private val newsChunkJpaRepository = mock(NewsChunkJpaRepository::class.java)
    private val repository = NewsChunkRepository(newsChunkJpaRepository)

    @Test
    fun `종목을 찾았으면 종목 지정 쿼리로 조건을 넘긴다`() {
        stubByStock(emptyList())

        repository.findSimilar(floatArrayOf(0.1f, 0.2f), stockId = 5L, articleLimit = 3, minScore = 0.8)

        verify(newsChunkJpaRepository).findSimilarByStock(
            // pgvector는 '[0.1,0.2,...]' 문자열을 vector로 캐스팅해 받는다.
            queryVector = "[0.1,0.2]",
            stockId = 5L,
            // 쿼리는 거리로 비교한다. 거리 = 1 - 유사도라 방향이 뒤집히며,
            // 헷갈리면 타입 오류 없이 결과만 정반대가 된다.
            maxDistance = 1.0 - 0.8,
            // 기사 단위로 접기 전이라 요청한 건수(3)보다 넉넉히 뽑는다.
            chunkLimit = 15,
        )
    }

    @Test
    fun `종목을 못 찾았으면 전역 검색 쿼리를 쓴다`() {
        stubAcrossStocks(emptyList())

        repository.findSimilar(floatArrayOf(0.1f), stockId = null, articleLimit = 3, minScore = 0.8)

        // 조건이 고정된 쿼리 두 벌로 나뉘어 있다. 한 벌로 합치면 플래너가 stock_id 인덱스를 못 쓴다.
        verify(newsChunkJpaRepository).findSimilarAcrossStocks(
            queryVector = "[0.1]",
            maxDistance = 1.0 - 0.8,
            chunkLimit = 15,
        )
    }

    @Test
    fun `같은 기사의 청크가 여러 개 걸려도 가장 가까운 하나만 남긴다`() {
        stubAcrossStocks(
            listOf(
                similarity(newsId = 1L, score = 0.95),
                similarity(newsId = 1L, score = 0.90),
                similarity(newsId = 2L, score = 0.85),
            ),
        )

        val hits = repository.findSimilar(floatArrayOf(0.1f), stockId = null, articleLimit = 10, minScore = 0.8)

        assertEquals(listOf(1L, 2L), hits.map { it.newsId })
        assertEquals(0.95, hits.first().score)
    }

    @Test
    fun `articleLimit은 청크가 아니라 기사 기준으로 적용된다`() {
        stubAcrossStocks(
            listOf(
                similarity(newsId = 1L, score = 0.95),
                similarity(newsId = 1L, score = 0.94),
                similarity(newsId = 2L, score = 0.90),
                similarity(newsId = 3L, score = 0.85),
            ),
        )

        val hits = repository.findSimilar(floatArrayOf(0.1f), stockId = null, articleLimit = 2, minScore = 0.8)

        // 중복 청크가 정원을 잡아먹으면 안 된다.
        assertEquals(listOf(1L, 2L), hits.map { it.newsId })
    }

    private fun stubByStock(result: List<NewsChunkSimilarity>) {
        `when`(
            newsChunkJpaRepository.findSimilarByStock(
                anyArg(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyDouble(),
                ArgumentMatchers.anyInt(),
            ),
        ).thenReturn(result)
    }

    private fun stubAcrossStocks(result: List<NewsChunkSimilarity>) {
        `when`(
            newsChunkJpaRepository.findSimilarAcrossStocks(
                anyArg(),
                ArgumentMatchers.anyDouble(),
                ArgumentMatchers.anyInt(),
            ),
        ).thenReturn(result)
    }

    private fun similarity(newsId: Long, score: Double): NewsChunkSimilarity =
        object : NewsChunkSimilarity {
            override fun getNewsId(): Long = newsId
            override fun getTitle(): String = "제목 $newsId"
            override fun getContent(): String = "본문 $newsId"
            override fun getUrl(): String = "https://example.com/$newsId"
            override fun getScore(): Double = score
        }
}
