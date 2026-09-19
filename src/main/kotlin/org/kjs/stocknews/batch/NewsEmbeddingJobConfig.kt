package org.kjs.stocknews.batch

import org.kjs.stocknews.model.dto.NewsEmbeddingItem
import org.kjs.stocknews.model.table.StockNews
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.service.NewsEmbeddingClient
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.interceptor.DefaultTransactionAttribute
import java.time.LocalDateTime

// StepExecution 컨텍스트에 이번 실행이 처리한 묶음 수를 담아두는 키.
private const val PROCESSED_BATCHES_KEY = "newsEmbedding.processedBatches"

// [배치] TB_STOCK_NEWS에서 아직 임베딩되지 않은 뉴스(EMBEDDED_AT IS NULL)를 batch-size(기본 10)건씩
// 임베딩 서비스(POST /v1/news)로 넘기고, 200을 받은 묶음만 EMBEDDED_AT을 찍어 큐에서 뺀다.
//
// 전송 단위를 작게 유지하는 이유: 임베딩은 CPU 추론이라 묶음이 커질수록 한 요청이 오래 블로킹되고,
// 중간에 실패하면 그 묶음 전체를 다시 보내야 한다(서비스가 건별 성공/실패를 돌려주지 않는 계약).
// 재전송해도 서비스가 news_id 기준 upsert라 중복 적재되지 않는다.
//
// 한 번 트리거될 때 최대 max-batches-per-run 묶음까지만 처리한다 - 수집 직후처럼 밀린 건이 많아도
// 한 실행이 무한정 길어지지 않게 하는 상한이다(남은 건은 다음 주기에 이어서 처리).
//
// 보존 정책 주의: StockNewsCleanupScheduler가 7일 지난 TB_STOCK_NEWS 행을 지우지만 벡터 DB는
// 건드리지 않는다. 즉 원본이 사라진 뒤에도 임베딩은 남는다(검색 결과에서 원본 링크만 따라가는 구조라
// 지금은 문제가 되지 않는다). 원본 기준으로 벡터를 정리해야 한다면 임베딩 서비스 쪽에 삭제 API를
// 두고 cleanup 배치가 같이 호출하도록 해야 한다.
@Configuration
class NewsEmbeddingJobConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val stockNewsRepository: StockNewsRepository,
    private val newsEmbeddingClient: NewsEmbeddingClient,
    @Value("\${news.embedding.batch-size}") private val batchSize: Int,
    @Value("\${news.embedding.max-batches-per-run}") private val maxBatchesPerRun: Int,
) {
    private val log = LoggerFactory.getLogger(NewsEmbeddingJobConfig::class.java)

    // Job: newsEmbeddingStep 단일 스텝으로 구성된 뉴스 임베딩 전송 배치 잡.
    @Bean
    fun newsEmbeddingJob(newsEmbeddingStep: Step): Job =
        JobBuilder("newsEmbeddingJob", jobRepository)
            .start(newsEmbeddingStep)
            .build()

    // Step: tasklet을 RepeatStatus.CONTINUABLE이 유지되는 동안 반복 실행한다.
    //
    // 스텝 트랜잭션을 PROPAGATION_NOT_SUPPORTED로 끈다. 기본값이면 임베딩 서비스의 HTTP 응답을
    // 기다리는 최대 read-timeout(3분) 동안 커넥션과 영속성 컨텍스트를 붙잡고 있게 된다.
    // 완료 표시(markEmbedded)는 리포지토리의 자체 트랜잭션으로 짧게 커밋되고, 태스크릿 자체가
    // 재전송에 안전(서비스가 news_id 기준 upsert)하므로 스텝 단위 롤백이 필요 없다.
    @Bean
    fun newsEmbeddingStep(newsEmbeddingTasklet: Tasklet): Step =
        StepBuilder("newsEmbeddingStep", jobRepository)
            .tasklet(newsEmbeddingTasklet, transactionManager)
            .transactionAttribute(DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_NOT_SUPPORTED))
            .build()

    // Tasklet: 미전송 뉴스를 batch-size만큼 꺼내 임베딩 서비스로 보내고, 200을 받은 건만 완료 표시한다.
    // 서비스가 5xx(503 제외)나 4xx를 주면 예외가 그대로 올라가 잡이 FAILED로 끝나고, 표시되지 않은
    // 건들은 다음 주기에 다시 대상이 된다.
    @Bean
    fun newsEmbeddingTasklet(): Tasklet = Tasklet { contribution, chunkContext ->
        // 처리한 묶음 수는 StepExecution 컨텍스트에 둔다. 스텝/태스크릿 빈은 싱글턴이라
        // 지역 변수로 들고 있으면 앞선 실행의 값이 다음 실행까지 따라온다.
        val executionContext = chunkContext.stepContext.stepExecution.executionContext
        val processedBatches = executionContext.getInt(PROCESSED_BATCHES_KEY, 0)

        val candidates = stockNewsRepository.findByEmbeddedAtIsNullOrderByIdAsc(PageRequest.of(0, batchSize))
        if (candidates.isEmpty()) {
            log.info("no news to embed, finishing (processedBatches={})", processedBatches)
            return@Tasklet RepeatStatus.FINISHED
        }

        // 본문이 비어 보낼 게 없는 건은 전송하지 않고 바로 완료 표시한다. 그냥 두면 이 건들이
        // 매 실행마다 큐 맨 앞(ID 오름차순)을 차지해 뒤의 정상 뉴스가 영영 처리되지 않는다.
        val (sendable, blank) = candidates.partition { !it.content.isNullOrBlank() }
        if (blank.isNotEmpty()) {
            log.info("marking {} news with blank content as embedded without sending", blank.size)
            markEmbedded(blank)
        }

        // 전송 없이 넘어간 묶음도 상한에 포함시킨다. 전송 성공 때만 세면 빈 본문이 대량으로 쌓였을 때
        // 한 실행이 그 전부를 소진할 때까지 끝나지 않는다.
        val nextProcessedBatches = processedBatches + 1
        executionContext.putInt(PROCESSED_BATCHES_KEY, nextProcessedBatches)

        if (sendable.isEmpty()) {
            // 이번 묶음이 전부 빈 본문이었던 경우. 다음 묶음을 이어서 본다.
            return@Tasklet continueUnlessLimitReached(nextProcessedBatches)
        }

        val response = newsEmbeddingClient.ingest(sendable.map(::toItem))
        if (response == null) {
            // 503 - 서비스 미준비(모델 워밍업 중이거나 벡터 DB 미연결). 장애가 아니므로 완료 표시 없이
            // 이번 실행만 정상 종료하고 다음 주기에 같은 건을 다시 보낸다.
            log.info("embedding service unavailable, stopping this run (processedBatches={})", processedBatches)
            return@Tasklet RepeatStatus.FINISHED
        }

        markEmbedded(sendable)
        contribution.incrementWriteCount(sendable.size.toLong())
        log.info(
            "sent {} news (embedded={} skipped={} chunks={}), batches {}/{}",
            sendable.size,
            response.embedded,
            response.skipped,
            response.chunks,
            nextProcessedBatches,
            maxBatchesPerRun,
        )

        continueUnlessLimitReached(nextProcessedBatches)
    }

    private fun continueUnlessLimitReached(processedBatches: Int): RepeatStatus {
        if (processedBatches >= maxBatchesPerRun) {
            log.info("reached max-batches-per-run={}, finishing", maxBatchesPerRun)
            return RepeatStatus.FINISHED
        }
        return RepeatStatus.CONTINUABLE
    }

    private fun markEmbedded(newsList: List<StockNews>) {
        val now = LocalDateTime.now()
        newsList.forEach { it.embeddedAt = now }
        stockNewsRepository.saveAll(newsList)
    }

    // content가 blank가 아님을 호출 전에 확인한 건만 들어오므로 non-null 단언이 안전하다.
    private fun toItem(news: StockNews) = NewsEmbeddingItem(
        id = news.id!!,
        title = news.title,
        content = news.content!!,
        url = news.url,
        stockId = news.stockId,
    )
}
