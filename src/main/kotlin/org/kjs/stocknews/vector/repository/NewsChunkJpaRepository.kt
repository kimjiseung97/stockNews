package org.kjs.stocknews.vector.repository

import org.kjs.stocknews.vector.table.NewsChunk
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

// 질의 벡터와 가까운 청크를 찾는 네이티브 조회.
//
// JPQL로는 표현할 수 없다 - <=>(코사인 거리)와 vector 타입은 pgvector 확장이라 Hibernate가 모른다.
// 그래서 거리 계산까지 DB 안에서 끝내고 인터페이스 프로젝션으로 받는다.
//
// JpaRepository가 아니라 Repository를 상속하는 이유: news_chunks의 소유자는 파이썬
// embedding-service다. JpaRepository를 쓰면 save/delete/deleteAll이 그대로 노출돼,
// 남의 테이블을 실수로 건드릴 통로가 생긴다. 여기서는 조회 메서드만 연다.
interface NewsChunkJpaRepository : Repository<NewsChunk, Long> {
    /**
     * 종목을 지정한 검색. 필터를 SQL 안에 둔다 - 전역 상위 N건을 받아 애플리케이션에서 거르면
     * 그 N건에 해당 종목이 하나도 없어 결과가 통째로 비는 일이 생긴다.
     *
     * 별칭에 큰따옴표를 씌운 이유: Postgres는 따옴표 없는 식별자를 소문자로 내려준다.
     * newsId가 newsid로 오면 인터페이스 프로젝션의 게터와 이름이 어긋난다.
     *
     * queryVector는 '[0.1,0.2,...]' 형태의 문자열이다(pgvector가 vector로 캐스팅해 받는다).
     *
     * chunkLimit은 **청크 행** 개수다. 기사 개수가 아니다 - 기사 하나가 여러 청크를 차지할 수
     * 있어서, 호출 측이 원하는 기사 수보다 넉넉히 잡아야 접은 뒤에 모자라지 않는다.
     */
    @Query(
        value = """
            SELECT news_id AS "newsId",
                   title AS "title",
                   content AS "content",
                   url AS "url",
                   1 - (embedding <=> CAST(:queryVector AS vector)) AS "score"
            FROM news_chunks
            WHERE stock_id = :stockId
              AND (embedding <=> CAST(:queryVector AS vector)) <= :maxDistance
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :chunkLimit
        """,
        nativeQuery = true,
    )
    fun findSimilarByStock(
        @Param("queryVector") queryVector: String,
        @Param("stockId") stockId: Long,
        @Param("maxDistance") maxDistance: Double,
        @Param("chunkLimit") chunkLimit: Int,
    ): List<NewsChunkSimilarity>

    /**
     * 종목을 특정하지 못한 질문("반도체 업황 어때?")용 전역 검색.
     *
     * 위 쿼리와 한 벌로 합쳐 `(:stockId IS NULL OR stock_id = :stockId)`로 쓸 수도 있지만,
     * 그러면 플래너가 어느 쪽 분기인지 확정하지 못해 stock_id 인덱스를 쓰는 계획을 고를 수 없다.
     * SQL이 조금 중복되더라도 조건이 고정된 쿼리 두 벌이 낫다.
     */
    @Query(
        value = """
            SELECT news_id AS "newsId",
                   title AS "title",
                   content AS "content",
                   url AS "url",
                   1 - (embedding <=> CAST(:queryVector AS vector)) AS "score"
            FROM news_chunks
            WHERE (embedding <=> CAST(:queryVector AS vector)) <= :maxDistance
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :chunkLimit
        """,
        nativeQuery = true,
    )
    fun findSimilarAcrossStocks(
        @Param("queryVector") queryVector: String,
        @Param("maxDistance") maxDistance: Double,
        @Param("chunkLimit") chunkLimit: Int,
    ): List<NewsChunkSimilarity>
}

// 네이티브 쿼리 결과 투영. score는 테이블 컬럼이 아니라 계산값이라 엔티티로는 받을 수 없다.
// 게터 이름이 SELECT의 별칭과 일치해야 매핑된다.
interface NewsChunkSimilarity {
    fun getNewsId(): Long
    fun getTitle(): String
    fun getContent(): String
    fun getUrl(): String

    // 1 - 코사인 거리. 1에 가까울수록 질문과 가깝다.
    fun getScore(): Double
}
