package org.kjs.stocknews.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.dto.NewsChunkHit
import org.kjs.stocknews.model.dto.NewsSearchArticle
import org.kjs.stocknews.model.dto.NewsSearchResponse
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

private fun <T> anyArg(): T = ArgumentMatchers.any()
private fun <T> eqArg(value: T): T = ArgumentMatchers.eq(value)

// articleLimit은 프리미티브라 any()가 돌려주는 null을 언박싱하다 NPE가 난다. 그래서 anyInt()를 쓴다.
// stockId는 nullable(Long?)이라 any()로 충분하다.
private fun anyLimit(): Int = ArgumentMatchers.anyInt()

private const val ARTICLE_LIMIT = 3

class NewsVectorSearchServiceTest {
    private val newsSearchClient = mock(NewsSearchClient::class.java)
    private val service = NewsVectorSearchService(newsSearchClient, articleLimit = ARTICLE_LIMIT)

    private fun article(newsId: Long) = NewsSearchArticle(
        newsId = newsId,
        title = "제목$newsId",
        content = "본문$newsId",
        url = "https://example.com/$newsId",
        score = 0.79,
    )

    @Test
    fun `질문 원문과 종목과 기사 수를 그대로 검색 서비스에 넘긴다`() {
        `when`(newsSearchClient.search(anyArg(), anyArg(), anyLimit()))
            .thenReturn(NewsSearchResponse(emptyList()))

        service.search("애플 실적 어때?", 1L)

        // 프리픽스("query: ")를 붙이는 것도 임베딩도 저쪽 일이다. 여기서 질문을 가공하면
        // 적재 쪽과 프리픽스가 이중으로 붙어 검색 품질이 조용히 망가진다.
        verify(newsSearchClient).search(eqArg("애플 실적 어때?"), eqArg(1L), eqArg(ARTICLE_LIMIT))
    }

    @Test
    fun `종목을 못 찾은 질문은 null로 넘겨 전체에서 찾게 한다`() {
        `when`(newsSearchClient.search(anyArg(), anyArg(), anyLimit()))
            .thenReturn(NewsSearchResponse(emptyList()))

        service.search("반도체 업황 어때?", null)

        verify(newsSearchClient).search(eqArg("반도체 업황 어때?"), eqArg(null), eqArg(ARTICLE_LIMIT))
    }

    @Test
    fun `응답의 기사를 그대로 변환해 돌려준다`() {
        `when`(newsSearchClient.search(anyArg(), anyArg(), anyLimit()))
            .thenReturn(NewsSearchResponse(listOf(article(1L), article(2L))))

        val hits = service.search("애플 실적 어때?", 1L)

        assertEquals(
            listOf(
                NewsChunkHit(1L, "제목1", "본문1", "https://example.com/1", 0.79),
                NewsChunkHit(2L, "제목2", "본문2", "https://example.com/2", 0.79),
            ),
            hits,
        )
    }

    @Test
    fun `검색 서비스가 아직 준비되지 않았으면 빈 결과를 돌려준다`() {
        // 클라이언트가 503을 null로 바꿔 올린다. 장애가 아니라 "지금은 못 준다"라서
        // 뉴스 근거 없이 답변을 이어가야 한다.
        `when`(newsSearchClient.search(anyArg(), anyArg(), anyLimit())).thenReturn(null)

        assertTrue(service.search("애플 실적 어때?", 1L).isEmpty())
    }

    @Test
    fun `검색 호출이 실패해도 예외 대신 빈 결과를 돌려준다`() {
        // 타임아웃, 주소 오타, 서비스 다운 등. 챗봇 전체가 500이 되면 안 되고
        // 뉴스 근거 없이 답변하는 쪽으로 떨어져야 한다.
        doThrow(RuntimeException("connection refused"))
            .`when`(newsSearchClient).search(anyArg(), anyArg(), anyLimit())

        assertTrue(service.search("애플 실적 어때?", 1L).isEmpty())
    }
}
