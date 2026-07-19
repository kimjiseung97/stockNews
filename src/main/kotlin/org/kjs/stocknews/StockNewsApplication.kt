package org.kjs.stocknews

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.batch.autoconfigure.BatchJobLauncherAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableBatchProcessing
@EnableJdbcJobRepository
@SpringBootApplication(exclude = [BatchJobLauncherAutoConfiguration::class])
class StockNewsApplication

fun main(args: Array<String>) {
    runApplication<StockNewsApplication>(*args)
}
