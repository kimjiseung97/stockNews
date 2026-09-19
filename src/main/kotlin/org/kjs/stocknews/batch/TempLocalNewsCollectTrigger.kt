package org.kjs.stocknews.batch

import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

// 임시 검증용: 429 여부를 직접 확인하기 위해 local 프로필 기동 시 1회만 news-collect 잡을 즉시 실행한다.
// 검증 끝나면 이 파일은 삭제할 것 (커밋 금지).
// gradlew test도 spring.profiles.active=local로 뜨는 바람에 @SpringBootTest마다 이 트리거가 같이 실행되어
// 실제 외부 API(Naver/Finnhub/SEC)를 두들기면서 테스트가 오래 걸리는 원인이 되어 임시로 비활성화.
// @Profile("local")
// @Component
class TempLocalNewsCollectTrigger(
    private val jobOperator: JobOperator,
    @Qualifier("stockNewsCollectJob") private val stockNewsCollectJob: Job,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(TempLocalNewsCollectTrigger::class.java)

    override fun run(args: ApplicationArguments) {
        log.info("TEMP: triggering stockNewsCollectJob immediately for local verification")
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        jobOperator.start(stockNewsCollectJob, jobParameters)
    }
}
