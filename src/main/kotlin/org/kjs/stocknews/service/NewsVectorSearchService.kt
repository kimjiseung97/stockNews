package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NewsChunkHit
import org.kjs.stocknews.repository.NewsChunkRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

// 챗봇 질문을 벡터로 바꿔 pgvector에서 비슷한 뉴스를 찾는다.
//
// 적재(뉴스 본문 임베딩)는 파이썬 embedding-service가 하고, 여기서는 "질문 한 문장"만 임베딩한다.
// 그래서 양쪽이 반드시 같은 모델이어야 한다 - 모델이 다르면 차원이 같아도 좌표계가 달라서
// 검색 결과가 조용히 무의미해진다(에러가 나지 않아 더 위험하다).
//
// e5 계열 모델은 입력 앞에 붙는 프리픽스를 전제로 학습돼 있다. 적재는 "passage: ",
// 검색은 "query: "를 붙여야 하며, 둘을 같게 쓰면 정확도가 눈에 띄게 떨어진다.
@Service
class NewsVectorSearchService(
    // 주입 지점에도 @Lazy가 있어야 한다. 빈 정의만 @Lazy로 두면 이 생성자가 참조하는 순간
    // 컨텍스트 기동 중에 실제 모델이 만들어져 수백 MB를 힙에 올린다(그대로 OOM으로 이어졌다).
    // 여기서는 프록시만 받아두고, 첫 질문이 들어올 때 실제 모델이 만들어진다.
    @Lazy private val embeddingModel: EmbeddingModel,
    private val newsChunkRepository: NewsChunkRepository,
    @Value("\${news.vector-search.query-prefix}") private val queryPrefix: String,
    // 프롬프트에 넣을 **기사** 개수다(청크 개수가 아니다).
    @Value("\${news.vector-search.article-limit}") private val articleLimit: Int,
    // 코사인 유사도 하한. e5 계열은 무관한 문장끼리도 0.7대가 나와서 낮게 잡으면 필터 구실을 못 하고
    // 엉뚱한 뉴스가 프롬프트 근거로 들어간다.
    @Value("\${news.vector-search.min-score}") private val minScore: Double,
) {
    private val log = LoggerFactory.getLogger(NewsVectorSearchService::class.java)

    // 질문과 의미가 가까운 뉴스를 종목으로 좁혀 찾는다. 실패하거나 결과가 없으면 빈 리스트.
    //
    // 구간별 소요시간을 남긴다. RAG 전환 이후 챗봇 응답이 수 초에서 수십 초로 늘었는데, 질의 임베딩과
    // pgvector 조회 중 어느 쪽이 그 시간을 쓰는지 코드만 봐서는 가를 수 없어 숫자로 확인하기 위한 것이다.
    //
    // 읽을 때 주의: 임베딩 모델은 @Lazy라 컨테이너 기동 후 **첫 호출**에 535MB 모델 적재가 포함된다.
    // 첫 줄은 무조건 큰 값이 나오므로 두 번째 질문부터 비교할 것.
    fun search(question: String, stockId: Long?): List<NewsChunkHit> {
        try {
            val embedStartedAt = System.nanoTime()
            val queryVector = embeddingModel.embed(queryPrefix + question)
            val embedFinishedAt = System.nanoTime()

            val hits = newsChunkRepository.findSimilar(queryVector, stockId, articleLimit, minScore)
            val searchFinishedAt = System.nanoTime()

            log.info(
                "news vector search timing: embedMs={} pgvectorMs={} hits={}",
                elapsedMs(embedStartedAt, embedFinishedAt),
                elapsedMs(embedFinishedAt, searchFinishedAt),
                hits.size,
            )
            return hits
        } catch (e: Exception) {
            // 여기서 예외를 올리면 벡터 DB나 모델이 잠깐 삐끗한 것만으로 챗봇 전체가 실패한다.
            // 뉴스 컨텍스트는 답변 품질을 올리는 보조 재료라 없으면 없는 대로 답변하는 편이 낫다.
            // 다만 전부 같은 모양으로 삼키면 일시적 장애와 설정 오류(잘못된 접속 정보, 차원 불일치)가
            // 구분되지 않으므로 예외 종류와 스택을 남긴다.
            log.warn("news vector search failed, answering without news context ({})", e.javaClass.simpleName, e)
            return emptyList()
        }
    }

    // 구간 측정에는 currentTimeMillis가 아니라 nanoTime을 쓴다. nanoTime은 단조 증가라
    // NTP 시계 조정이 끼어들어도 음수나 엉뚱한 값이 나오지 않는다.
    private fun elapsedMs(fromNanos: Long, toNanos: Long): Long = (toNanos - fromNanos) / 1_000_000
}
