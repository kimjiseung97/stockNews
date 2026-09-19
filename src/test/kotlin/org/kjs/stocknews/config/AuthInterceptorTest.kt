package org.kjs.stocknews.config

import org.junit.jupiter.api.Test
import org.kjs.stocknews.common.SessionKeys
import org.kjs.stocknews.model.dto.StockChatResponse
import org.kjs.stocknews.service.StockChatService
import org.kjs.stocknews.service.UserStockService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

// Mockito의 any()는 널을 반환해 Kotlin non-null 파라미터 콜사이트에서 NPE를 유발한다.
// 로컬 래퍼로 정적 반환 타입을 non-null로 감춰서 우회한다.
private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

@SpringBootTest
@AutoConfigureMockMvc
class AuthInterceptorTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var userStockService: UserStockService

    @MockitoBean
    private lateinit var stockChatService: StockChatService

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

    // 챗봇은 호출 1건마다 NVIDIA LLM 토큰 비용이 나간다. 프론트가 비로그인 시 입력창을 비활성화하지만
    // 그건 화면 가드일 뿐이라, 서버에서도 막히는지 확인한다(회귀하면 곧바로 비용으로 이어진다).
    @Test
    fun `세션이 없으면 챗봇 호출은 UNAUTHORIZED로 거부된다`() {
        mockMvc.perform(
            post("/stocks/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"question":"애플 어때?"}"""),
        )
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
    }

    @Test
    fun `세션이 있으면 챗봇 호출이 정상 처리된다`() {
        org.mockito.Mockito.`when`(stockChatService.ask(anyArg()))
            .thenReturn(StockChatResponse("답변"))

        val session = MockHttpSession()
        session.setAttribute(SessionKeys.USER_ID, 1L)

        mockMvc.perform(
            post("/stocks/chat")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"question":"애플 어때?"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("OK"))
    }
}
