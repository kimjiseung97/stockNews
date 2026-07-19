package org.kjs.stocknews

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.batch.autoconfigure.BatchJobLauncherAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication(exclude = [BatchJobLauncherAutoConfiguration::class])
class StockNewsApplication

fun main(args: Array<String>) {
    runApplication<StockNewsApplication>(*args)
}
