package org.kjs.stocknews.controller

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/stocks")
class StockAdminController(
    private val jobOperator: JobOperator,
    @Qualifier("stockSeedJob") private val stockSeedJob: Job,
    @Qualifier("stockThemeEnrichJob") private val stockThemeEnrichJob: Job,
) {

    @PostMapping("/seed")
    fun seed(): String {
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(stockSeedJob, jobParameters)
        return "started jobExecutionId=${execution.id}"
    }

    @PostMapping("/enrich-theme")
    fun enrichTheme(): String {
        val jobParameters = JobParametersBuilder()
            .addLong("triggeredAt", System.currentTimeMillis())
            .toJobParameters()
        val execution = jobOperator.start(stockThemeEnrichJob, jobParameters)
        return "started jobExecutionId=${execution.id}"
    }
}
