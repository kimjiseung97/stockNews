package org.kjs.stocknews.batch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.kjs.stocknews.model.dto.NewsEmbeddingItem
import org.kjs.stocknews.model.dto.NewsEmbeddingResponse
import org.kjs.stocknews.model.table.StockNews
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.service.NewsEmbeddingClient
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.scope.context.StepContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.batch.test.MetaDataInstanceFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.client.HttpServerErrorException

private const val BATCH_SIZE = 10
private const val MAX_BATCHES_PER_RUN = 3

// Mockito의 any()는 널을 반환해 Kotlin non-null 파라미터 콜사이트에서 NPE를 유발한다.
// 로컬 래퍼로 정적 반환 타입을 non-null로 감춰서 우회한다.
private fun <T> anyArg(): T = ArgumentMatchers.any()

class NewsEmbeddingJobConfigTest {
    private val stockNewsRepository = mock(StockNewsRepository::class.java)
    private val newsEmbeddingClient = mock(NewsEmbeddingClient::class.java)

    private val config = NewsEmbeddingJobConfig(
        jobRepository = mock(JobRepository::class.java),
        transactionManager = mock(PlatformTransactionManager::class.java),
        stockNewsRepository = stockNewsRepository,
        newsEmbeddingClient = newsEmbeddingClient,
        batchSize = BATCH_SIZE,
        maxBatchesPerRun = MAX_BATCHES_PER_RUN,
    )

    // 태스크릿은 스텝 실행 컨텍스트에 묶음 수를 누적하므로, 반복 호출 테스트에서 같은 컨텍스트를 재사용해야 한다.
    private val chunkContext = ChunkContext(StepContext(MetaDataInstanceFactory.createStepExecution()))

    private fun execute(): RepeatStatus? =
        config.newsEmbeddingTasklet().execute(
            StepContribution(chunkContext.stepContext.stepExecution),
            chunkContext,
        )

    private fun news(id: Long, content: String?): StockNews {
        val stockNews = StockNews(
            stockId = 10L,
            title = "제목 $id",
            content = content,
            url = "https://example.com/$id",
        )
        // id는 protected set이라 테스트에서 직접 채울 수 없다.
        ReflectionTestUtils.setField(stockNews, "id", id)
        return stockNews
    }

    private fun candidatesReturning(vararg batches: List<StockNews>) {
        var stub = `when`(stockNewsRepository.findByEmbeddedAtIsNullOrderByIdAsc(anyArg<Pageable>()))
        batches.forEach { stub = stub.thenReturn(it) }
    }

    // ArgumentCaptor.capture()는 널을 반환해 Kotlin non-null 파라미터에서 NPE가 나므로,
    // 전송된 묶음은 스텁 안에서 직접 기록한다.
    private val sentBatches = mutableListOf<List<NewsEmbeddingItem>>()

    private fun ingestReturning(response: NewsEmbeddingResponse?) {
        `when`(newsEmbeddingClient.ingest(anyArg())).thenAnswer { invocation ->
            sentBatches.add(invocation.getArgument(0))
            response
        }
    }

    @Test
    fun `미임베딩 뉴스를 임베딩 서비스로 보내고 전송한 건만 완료 표시한다`() {
        val candidates = listOf(news(1L, "본문1"), news(2L, "본문2"))
        candidatesReturning(candidates)
        ingestReturning(NewsEmbeddingResponse(news = 2, embedded = 2, skipped = 0, chunks = 4))

        val status = execute()

        assertEquals(1, sentBatches.size)
        val sent = sentBatches.single()
        assertEquals(listOf(1L, 2L), sent.map { it.id })
        assertEquals(listOf(10L, 10L), sent.map { it.stockId })
        assertEquals("본문1", sent[0].content)

        candidates.forEach { assertNotNull(it.embeddedAt) }
        verify(stockNewsRepository).saveAll(candidates)
        // 아직 max-batches-per-run에 닿지 않았으므로 다음 묶음을 이어서 본다.
        assertEquals(RepeatStatus.CONTINUABLE, status)
    }

    @Test
    fun `대상이 없으면 전송 없이 종료한다`() {
        candidatesReturning(emptyList())

        val status = execute()

        verify(newsEmbeddingClient, never()).ingest(anyArg())
        assertEquals(RepeatStatus.FINISHED, status)
    }

    @Test
    fun `본문이 비어 보낼 게 없는 건은 전송하지 않고 완료 표시해 큐에서 뺀다`() {
        val blank = news(1L, null)
        val empty = news(2L, "   ")
        val sendable = news(3L, "본문")
        candidatesReturning(listOf(blank, empty, sendable))
        ingestReturning(NewsEmbeddingResponse(news = 1, embedded = 1, skipped = 0, chunks = 1))

        execute()

        assertEquals(listOf(3L), sentBatches.single().map { it.id })
        // 보내지 않은 빈 본문 건도 완료 표시해야 다음 실행에서 큐 맨 앞을 다시 차지하지 않는다.
        assertNotNull(blank.embeddedAt)
        assertNotNull(empty.embeddedAt)
    }

    @Test
    fun `서비스가 준비되지 않았으면 완료 표시 없이 이번 실행만 종료한다`() {
        val candidates = listOf(news(1L, "본문"))
        candidatesReturning(candidates)
        ingestReturning(null)

        val status = execute()

        assertEquals(RepeatStatus.FINISHED, status)
        assertNull(candidates[0].embeddedAt)
        verify(stockNewsRepository, never()).saveAll(anyArg<List<StockNews>>())
    }

    @Test
    fun `서비스 장애는 삼키지 않고 잡을 실패시킨다`() {
        candidatesReturning(listOf(news(1L, "본문")))
        `when`(newsEmbeddingClient.ingest(anyArg()))
            .thenThrow(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "boom"))

        assertThrows<HttpServerErrorException> { execute() }
        verify(stockNewsRepository, never()).saveAll(anyArg<List<StockNews>>())
    }

    @Test
    fun `한 실행에서 max-batches-per-run 묶음까지만 처리한다`() {
        candidatesReturning(
            listOf(news(1L, "본문")),
            listOf(news(2L, "본문")),
            listOf(news(3L, "본문")),
        )
        ingestReturning(NewsEmbeddingResponse(news = 1, embedded = 1, skipped = 0, chunks = 1))

        assertEquals(RepeatStatus.CONTINUABLE, execute())
        assertEquals(RepeatStatus.CONTINUABLE, execute())
        // 3번째 묶음에서 상한에 닿아 더 남아 있어도 종료한다.
        assertEquals(RepeatStatus.FINISHED, execute())
        assertTrue(chunkContext.stepContext.stepExecution.executionContext.getInt("newsEmbedding.processedBatches") == MAX_BATCHES_PER_RUN)
    }
}
