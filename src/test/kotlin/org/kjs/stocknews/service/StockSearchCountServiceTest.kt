package org.kjs.stocknews.service

import org.junit.jupiter.api.Test
import org.kjs.stocknews.model.table.StockSearchCount
import org.kjs.stocknews.repository.StockSearchCountRepository
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

// Mockito의 eq()/any()는 널을 반환해 Kotlin non-null 파라미터 콜사이트에서 NPE를 유발한다.
private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

class StockSearchCountServiceTest {
    private val stockSearchCountRepository = mock(StockSearchCountRepository::class.java)
    private val stockSearchCountService = StockSearchCountService(stockSearchCountRepository)

    @Test
    fun `saveSearchCount은 적재 실패해도 예외를 전파하지 않는다`() {
        `when`(stockSearchCountRepository.save(anyArg<StockSearchCount>())).thenThrow(RuntimeException("db down"))

        stockSearchCountService.saveSearchCount(1L)
    }

    @Test
    fun `saveSearchCountAll은 stockIds가 비어있으면 저장을 호출하지 않는다`() {
        stockSearchCountService.saveSearchCountAll(emptyList())

        verify(stockSearchCountRepository, never()).saveAll(anyArg<List<StockSearchCount>>())
    }
}
