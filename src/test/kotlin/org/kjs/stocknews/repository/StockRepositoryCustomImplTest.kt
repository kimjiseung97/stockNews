package org.kjs.stocknews.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired

// findWithoutDetail이 RAND() 정렬을 타서 예외 없이 동작하고, 매 호출마다 순서가 바뀌는지 확인한다.
@SpringBootTest
class StockRepositoryCustomImplTest(
    @Autowired private val stockRepository: StockRepository,
) {

    @Test
    fun `findWithoutDetail은 RAND 정렬로 예외 없이 조회된다`() {
        val first = stockRepository.findWithoutDetail(20).map { it.id }
        val second = stockRepository.findWithoutDetail(20).map { it.id }

        assertThat(first).isNotEmpty
        assertThat(second).isNotEmpty
        assertThat(first).isNotEqualTo(second)
    }
}
