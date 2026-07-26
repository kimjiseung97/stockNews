package org.kjs.stocknews.config

import org.junit.jupiter.api.Test
import org.kjs.stocknews.common.SessionKeys
import org.kjs.stocknews.service.UserStockService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class AuthInterceptorTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var userStockService: UserStockService

    @Test
    fun `세션이 없으면 users me 경로 요청은 UNAUTHORIZED로 거부된다`() {
        mockMvc.perform(get("/users/me/stocks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `세션에 사용자 정보가 있으면 users me 경로 요청이 정상 처리된다`() {
        org.mockito.Mockito.`when`(userStockService.list(1L, PageRequest.of(0, 20)))
            .thenReturn(PageImpl(emptyList()))

        val session = MockHttpSession()
        session.setAttribute(SessionKeys.USER_ID, 1L)

        mockMvc.perform(get("/users/me/stocks").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("OK"))
    }
}
