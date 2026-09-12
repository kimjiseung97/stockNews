package org.kjs.stocknews.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

// SCHEDULER_ENABLED=false로 배치 스케줄러 전체를 끌 수 있다 (dev망에서 prod와 수집 중복 방지 용도).
@Configuration
@ConditionalOnProperty(name = ["scheduler.enabled"], havingValue = "true", matchIfMissing = true)
@EnableScheduling
class SchedulingConfig
