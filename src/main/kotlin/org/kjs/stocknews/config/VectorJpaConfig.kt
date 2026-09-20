package org.kjs.stocknews.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

// 벡터 DB(Postgres + pgvector)용 JPA 설정. 도메인 DB와 완전히 분리된 두 번째 영속성 단위다.
//
// 이 애플리케이션은 벡터를 읽기만 한다. 뉴스 본문 임베딩과 news_chunks 적재는 파이썬
// embedding-service가 담당한다.
@Configuration
@EnableJpaRepositories(
    basePackages = ["org.kjs.stocknews.vector.repository"],
    entityManagerFactoryRef = "vectorEntityManagerFactory",
    transactionManagerRef = "vectorTransactionManager",
)
class VectorJpaConfig {
    @Bean
    fun vectorEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("vectorDataSource") vectorDataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(vectorDataSource)
            .packages("org.kjs.stocknews.vector.table")
            .persistenceUnit("vector")
            .properties(
                mapOf(
                    // 반드시 none이어야 한다. 스키마 소유자는 파이썬 embedding-service이고,
                    // 전역 설정(spring.jpa.hibernate.ddl-auto)은 update라서 그대로 두면
                    // Hibernate가 남의 테이블을 고치려 든다. embedding 컬럼을 매핑하지 않았으므로
                    // 최악의 경우 vector(1024) 컬럼을 지우려 들 수도 있다.
                    "hibernate.hbm2ddl.auto" to "none",
                    // 커넥션에서 방언을 추론하려면 부팅 시점에 DB에 붙어야 한다. 벡터 DB가 늦게
                    // 떠도 앱 기동이 막히지 않도록 방언을 못 박는다.
                    "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
                ),
            )
            .build()

    @Bean
    fun vectorTransactionManager(
        @Qualifier("vectorEntityManagerFactory") vectorEntityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(vectorEntityManagerFactory)
}
