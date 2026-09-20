package org.kjs.stocknews.vector.table

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.OffsetDateTime

// 파이썬 embedding-service가 적재하는 pgvector 테이블. 이 애플리케이션은 읽기만 한다.
//
// 이 패키지를 org.kjs.stocknews.model.table 아래에 두지 않은 이유: 주 EntityManagerFactory가
// 그 패키지를 재귀로 스캔하므로, 하위에 두면 MariaDB 쪽에도 이 엔티티가 등록된다. ddl-auto가
// update라서 Hibernate가 MariaDB에 news_chunks를 만들려 든다.
//
// 스키마 소유자는 파이썬이다. 벡터용 EntityManagerFactory는 ddl-auto를 none으로 두어
// Hibernate가 이 테이블을 건드리지 못하게 한다(VectorJpaConfig).
@Entity
@Immutable
@Table(name = "news_chunks")
class NewsChunk(
    @Column(name = "news_id", nullable = false)
    val newsId: Long,

    @Column(name = "chunk_index", nullable = false)
    val chunkIndex: Int,

    @Column(name = "stock_id", nullable = false)
    val stockId: Long,

    @Column(name = "title", nullable = false)
    val title: String,

    // 기사 전문이 아니라 청크 하나다. 임베딩에는 "제목\n청크"를 쓰되 저장은 제목을 뺀 청크만 한다.
    @Column(name = "content", nullable = false)
    val content: String,

    @Column(name = "url", nullable = false)
    val url: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime,
) {
    // embedding vector(1024) 컬럼은 일부러 매핑하지 않는다. Hibernate가 모르는 확장 타입이라
    // 매핑하려면 별도 타입 구현이 필요한데, 조회 결과로 벡터를 꺼낼 일이 없다.
    // 유사도 계산은 네이티브 쿼리가 DB 안에서 끝낸다(NewsChunkJpaRepository).

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null
        protected set
}
