package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NewsChunkHit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

// 챗봇 질문과 의미가 가까운 뉴스를 찾는다.
//
// 임베딩과 pgvector 조회는 파이썬 embedding-service가 한다. 이 앱은 질문 문장을 넘기고
// 기사 목록을 받을 뿐이라 모델도 벡터 DB도 알지 못한다.
//
// 예전에는 이 클래스가 ONNX 모델을 JVM에 올려 직접 임베딩하고 pgvector에도 직접 붙었다.
// 모델 가중치가 힙이 아니라 네이티브 메모리에 잡혀 -Xmx로 통제되지 않았고, 실측에서
// 컨테이너가 상한의 99.94%를 점유하고 469MB가 스왑으로 밀려났으며 배포 후 첫 질문이
// 206초 걸렸다. 적재와 검색을 한 서비스가 맡는 편이 경계로도 맞다.
@Service
class NewsVectorSearchService(
    private val newsSearchClient: NewsSearchClient,
    // 프롬프트에 넣을 기사 건수. 늘리면 근거는 늘지만 LLM 토큰과 응답 지연이 함께 늘어난다.
    // 유사도 하한(min_score)은 여기서 보내지 않는다 - 인덱스와 모델을 가진 쪽이 정하는 게 맞고,
    // 실측상 그 값은 관련성 필터 구실을 못 해서 이 앱이 만질 손잡이가 아니다.
    @Value("\${news.vector-search.article-limit}") private val articleLimit: Int,
) {
    private val log = LoggerFactory.getLogger(NewsVectorSearchService::class.java)

    // 질문과 의미가 가까운 뉴스를 종목으로 좁혀 찾는다. 실패하거나 결과가 없으면 빈 리스트.
    fun search(question: String, stockId: Long?): List<NewsChunkHit> {
        try {
            // 소요시간과 결과 건수는 StockChatService가 newsSearchMs/articles로 남긴다.
            // 여기서 또 찍으면 같은 값이 두 줄이 되고, 이 서버는 배치 로그만으로도 이미
            // 필요한 줄이 밀려나는 상황이라 정상 경로에는 로그를 두지 않는다.
            val response = newsSearchClient.search(question, stockId, articleLimit)
                ?: return emptyList()

            return response.articles.map {
                NewsChunkHit(
                    newsId = it.newsId,
                    title = it.title,
                    content = it.content,
                    url = it.url,
                    score = it.score,
                )
            }
        } catch (e: Exception) {
            // 여기서 예외를 올리면 임베딩 서비스가 잠깐 삐끗한 것만으로 챗봇 전체가 실패한다.
            // 뉴스 컨텍스트는 답변 품질을 올리는 보조 재료라 없으면 없는 대로 답변하는 편이 낫다.
            // 다만 전부 같은 모양으로 삼키면 일시적 장애와 설정 오류(주소 오타 등)가 구분되지
            // 않으므로 예외 종류와 스택을 남긴다.
            log.warn("news search failed, answering without news context ({})", e.javaClass.simpleName, e)
            return emptyList()
        }
    }
}
