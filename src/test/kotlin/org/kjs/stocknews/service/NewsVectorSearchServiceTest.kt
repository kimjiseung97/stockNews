package org.kjs.stocknews.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.dto.NewsChunkHit
import org.kjs.stocknews.repository.NewsChunkRepository
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.ai.embedding.EmbeddingModel

private fun <T> anyArg(): T = ArgumentMatchers.any()
private fun <T> eqArg(value: T): T = ArgumentMatchers.eq(value)

// findSimilar의 limit/minScore는 프리미티브라 any()가 돌려주는 null을 언박싱하다 NPE가 난다.
// 그래서 타입별 매처(anyInt/anyDouble)를 쓴다. stockId는 nullable(Long?)이라 any()로 충분하다.

private const val QUERY_PREFIX = "query: "

class NewsVectorSearchServiceTest {
    private val embeddingModel = mock(EmbeddingModel::class.java)
    private val newsChunkRepository = mock(NewsChunkRepository::class.java)
    private val service = NewsVectorSearchService(
        embeddingModel,
        newsChunkRepository,
        queryPrefix = QUERY_PREFIX,
        articleLimit = 3,
        minScore = 0.8,
    )

    @Test
    fun `질문 앞에 query 프리픽스를 붙여 임베딩한다`() {
        `when`(embeddingModel.embed(anyArg<String>())).thenReturn(floatArrayOf(0.1f))
        `when`(newsChunkRepository.findSimilar(anyArg(), anyArg(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyDouble())).thenReturn(emptyList())

        service.search("애플 실적 어때?", 1L)

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(embeddingModel).embed(captor.capture())
        // 적재는 "passage: "로 들어가 있다. 검색에 같은 프리픽스를 쓰면 정확도가 눈에 띄게 떨어진다.
        assertEquals("query: 애플 실적 어때?", captor.value)
    }

    @Test
    fun `임베딩한 벡터와 설정값을 그대로 조회에 넘긴다`() {
        val vector = floatArrayOf(0.1f, 0.2f)
        `when`(embeddingModel.embed(anyArg<String>())).thenReturn(vector)
        `when`(newsChunkRepository.findSimilar(anyArg(), anyArg(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyDouble())).thenReturn(emptyList())

        service.search("애플 실적 어때?", 1L)

        verify(newsChunkRepository).findSimilar(eqArg(vector), eqArg(1L), eqArg(3), eqArg(0.8))
    }

    @Test
    fun `조회 결과를 그대로 돌려준다`() {
        val hits = listOf(
            NewsChunkHit(newsId = 1L, title = "제목", content = "본문", url = "https://example.com/1", score = 0.9),
        )
        `when`(embeddingModel.embed(anyArg<String>())).thenReturn(floatArrayOf(0.1f))
        `when`(newsChunkRepository.findSimilar(anyArg(), anyArg(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyDouble())).thenReturn(hits)

        assertEquals(hits, service.search("애플 실적 어때?", 1L))
    }

    @Test
    fun `모델 로딩이 실패해도 예외 대신 빈 결과를 돌려준다`() {
        // 모델은 첫 질문에 만들어진다(@Lazy). 그때 OOM이나 다운로드 실패가 나도 챗봇 전체가
        // 500이 되면 안 되고, 뉴스 근거 없이 답변하는 쪽으로 떨어져야 한다.
        doThrow(IllegalStateException("model load failed")).`when`(embeddingModel).embed(anyArg<String>())

        assertTrue(service.search("애플 실적 어때?", 1L).isEmpty())
    }

    @Test
    fun `벡터 DB 조회가 실패해도 예외 대신 빈 결과를 돌려준다`() {
        `when`(embeddingModel.embed(anyArg<String>())).thenReturn(floatArrayOf(0.1f))
        doThrow(RuntimeException("connection refused"))
            .`when`(newsChunkRepository).findSimilar(anyArg(), anyArg(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyDouble())

        assertTrue(service.search("애플 실적 어때?", 1L).isEmpty())
    }
}
