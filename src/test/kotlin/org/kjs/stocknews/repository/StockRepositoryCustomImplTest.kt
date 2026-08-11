package org.kjs.stocknews.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

// findWithoutDetail이 detailAttemptedAt 오름차순(NULL 우선)으로 조회되는지 확인한다.
@SpringBootTest
class StockRepositoryCustomImplTest(
    @Autowired private val stockRepository: StockRepository,
) {

    @Test
    fun `findWithoutDetail은 detailAttemptedAt이 이른 순으로 조회된다`() {
        val candidates = stockRepository.findWithoutDetail(20)
        assertThat(candidates).isNotEmpty

        val attemptedAts = candidates.map { it.detailAttemptedAt ?: LocalDateTime.MIN }
        assertThat(attemptedAts).isSorted
    }
}
