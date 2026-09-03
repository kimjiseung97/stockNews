package org.kjs.stocknews.common

import org.junit.jupiter.api.Test
import org.kjs.stocknews.service.AuthService
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.QueryTimeoutException
import org.springframework.transaction.UnexpectedRollbackException
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import java.net.SocketTimeoutException

/**
 * 전역 예외 처리기가 원인 계층별로 서로 다른 ResultCode를 내려주는지 검증한다.
 * 회귀 방지 대상: 예전에는 아래 케이스가 전부 INTERNAL_ERROR로 뭉개져 원인 구분이 불가능했다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var authService: AuthService

    @Test
    fun `요청 body가 아예 없으면 MALFORMED_REQUEST_BODY를 내려준다`() {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"))
    }

    @Test
    fun `요청 body에 non-null 필드가 누락되면 MALFORMED_REQUEST_BODY를 내려준다`() {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com"}"""),
        )
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"))
    }

    @Test
    fun `POST 전용 엔드포인트에 GET으로 요청하면 METHOD_NOT_ALLOWED를 내려준다`() {
        mockMvc.perform(get("/auth/login"))
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
    }

    @Test
    fun `DB 커넥션 획득에 실패하면 DATABASE_UNAVAILABLE을 내려준다`() {
        `when`(authService.login("user@example.com", "password1!"))
            .thenThrow(DataAccessResourceFailureException("connection refused"))

        mockMvc.perform(loginRequest())
            .andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE"))
    }

    @Test
    fun `쿼리 실행이 실패하면 DATABASE_ERROR를 내려준다`() {
        `when`(authService.login("user@example.com", "password1!"))
            .thenThrow(DataIntegrityViolationException("constraint violation"))

        mockMvc.perform(loginRequest())
            .andExpect(jsonPath("$.code").value("DATABASE_ERROR"))
    }

    @Test
    fun `쿼리가 제한 시간을 넘기면 DATABASE_TIMEOUT을 내려준다`() {
        `when`(authService.login("user@example.com", "password1!"))
            .thenThrow(QueryTimeoutException("statement timeout"))

        mockMvc.perform(loginRequest())
            .andExpect(jsonPath("$.code").value("DATABASE_TIMEOUT"))
    }

    @Test
    fun `트랜잭션 롤백 실패는 DATABASE_ERROR를 내려준다`() {
        `when`(authService.login("user@example.com", "password1!"))
            .thenThrow(UnexpectedRollbackException("marked rollback-only"))

        mockMvc.perform(loginRequest())
            .andExpect(jsonPath("$.code").value("DATABASE_ERROR"))
    }

    @Test
    fun `외부 API에 연결하지 못하면 EXTERNAL_API_UNAVAILABLE을 내려준다`() {
        `when`(authService.login("user@example.com", "password1!"))
            .thenThrow(ResourceAccessException("read timed out", SocketTimeoutException()))

        mockMvc.perform(loginRequest())
            .andExpect(jsonPath("$.code").value("EXTERNAL_API_UNAVAILABLE"))
    }

    @Test
    fun `외부 API 호출이 실패하면 EXTERNAL_API_ERROR를 내려준다`() {
        `when`(authService.login("user@example.com", "password1!"))
            .thenThrow(RestClientException("unexpected response"))

        mockMvc.perform(loginRequest())
            .andExpect(jsonPath("$.code").value("EXTERNAL_API_ERROR"))
    }

    @Test
    fun `분류되지 않은 예외는 기존대로 INTERNAL_ERROR를 내려준다`() {
        `when`(authService.login("user@example.com", "password1!"))
            .thenThrow(RuntimeException("boom"))

        mockMvc.perform(loginRequest())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
    }

    @Test
    fun `비즈니스 예외는 고유 ResultCode를 그대로 유지한다`() {
        `when`(authService.login("user@example.com", "password1!"))
            .thenThrow(BusinessException(ResultCode.INVALID_CREDENTIALS))

        mockMvc.perform(loginRequest())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
    }

    @Test
    fun `필수 쿼리 파라미터가 누락되면 BAD_REQUEST를 내려준다`() {
        mockMvc.perform(get("/auth/email/exists"))
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
    }

    private fun loginRequest() =
        post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"email":"user@example.com","password":"password1!"}""")
}
