package org.kjs.stocknews.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

// DataSource가 두 개다 - 도메인 DB(MariaDB, JPA/Batch용)와 벡터 DB(Postgres + pgvector, 읽기 전용).
//
// 주의: DataSource 빈을 직접 하나라도 만들면 Boot의 DataSource 자동설정이 통째로 물러난다
// (@ConditionalOnMissingBean(DataSource)). 그래서 도메인 DB도 여기서 명시적으로 만들고 @Primary를
// 붙여야 한다. 안 그러면 JPA/Batch가 남은 하나(벡터 DB)를 잡고 Postgres에 붙으려 한다.
//
// 벡터 적재는 이 애플리케이션이 하지 않는다. 뉴스 본문 임베딩과 news_chunks INSERT는 파이썬
// embedding-service가 담당하고, 여기서는 챗봇 질의로 검색만 한다.
@Configuration
class DataSourceConfig {
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    fun dataSourceProperties(): DataSourceProperties = DataSourceProperties()

    // spring.datasource.hikari.* (커넥션 풀 크기 등)를 그대로 유지하기 위해 프로퍼티 바인딩을 건다.
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    fun dataSource(dataSourceProperties: DataSourceProperties): DataSource =
        dataSourceProperties.initializeDataSourceBuilder()
            .type(HikariDataSource::class.java)
            .build()

    @Bean
    fun vectorDataSource(
        @Value("\${vector.datasource.url}") url: String,
        @Value("\${vector.datasource.username}") username: String,
        @Value("\${vector.datasource.password}") password: String,
        // 챗봇 질문당 한 번 쓰는 조회라 커넥션이 많이 필요 없다.
        @Value("\${vector.datasource.pool-size:3}") poolSize: Int,
        // HNSW 후보 폭. 기본값 40으로는 stock_id 필터가 걸린 검색에서 요청한 건수를 못 채운다 -
        // ANN이 유사도 상위 후보를 먼저 뽑고 필터는 그 뒤에 걸려서, 후보 안에 해당 종목 기사가
        // 하나도 없으면 결과가 0건이 된다.
        @Value("\${vector.datasource.hnsw-ef-search:200}") efSearch: Int,
    ): DataSource {
        val dataSource = DataSourceBuilder.create()
            .type(HikariDataSource::class.java)
            .driverClassName("org.postgresql.Driver")
            .url(url)
            .username(username)
            .password(password)
            .build()
        dataSource.maximumPoolSize = poolSize
        dataSource.poolName = "vector-pool"
        // 이 앱은 벡터를 읽기만 한다. 적재는 파이썬 embedding-service 담당이다.
        dataSource.isReadOnly = true
        // 커넥션을 새로 열 때마다 건다. 조회 직전에 별도로 SET을 날리면 풀에서 다른 커넥션을
        // 받을 수 있어 설정이 실제 쿼리에 적용되지 않는다.
        dataSource.connectionInitSql = "SET hnsw.ef_search = $efSearch"
        return dataSource
    }
}
