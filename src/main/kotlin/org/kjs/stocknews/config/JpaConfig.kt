package org.kjs.stocknews.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

// 도메인 DB(MariaDB)용 JPA 설정.
//
// 원래는 Boot 자동설정이 해주던 일이다. 벡터 DB용 EntityManagerFactory를 하나 더 만드는 순간
// JpaBaseConfiguration이 물러나므로(@ConditionalOnMissingBean(EntityManagerFactory)),
// 이쪽도 명시적으로 선언해야 한다. @Primary가 붙은 쪽이 기존 코드가 쓰던 그 EMF다.
//
// 저장소 패키지를 DB별로 나눈다. 벡터 저장소는 org.kjs.stocknews.vector.repository에 있어
// 여기 basePackages에 걸리지 않는다 - 한 패키지 아래 섞이면 어느 DB로 갈지 지정할 수 없다.
@Configuration
@EnableJpaRepositories(
    basePackages = ["org.kjs.stocknews.repository"],
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager",
)
class JpaConfig {
    // 빌더는 자동설정에 남아 있는 것을 그대로 쓴다. spring.jpa.* (ddl-auto, 네이밍 전략 등)가
    // 여기에 실려 있어, 직접 만들어도 기존 동작이 그대로 유지된다.
    @Bean
    @Primary
    fun entityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("dataSource") dataSource: DataSource,
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(dataSource)
            // 벡터 엔티티(org.kjs.stocknews.vector.table)는 여기 들어오면 안 된다.
            // ddl-auto가 update라 Hibernate가 MariaDB에 news_chunks를 만들려 든다.
            .packages("org.kjs.stocknews.model.table")
            .persistenceUnit("domain")
            .build()

    @Bean
    @Primary
    fun transactionManager(
        @Qualifier("entityManagerFactory") entityManagerFactory: EntityManagerFactory,
    ): PlatformTransactionManager = JpaTransactionManager(entityManagerFactory)
}
