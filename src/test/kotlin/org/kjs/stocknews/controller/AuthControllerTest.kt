package org.kjs.stocknews.controller

import org.junit.jupiter.api.Test
import org.kjs.stocknews.service.AuthService
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @MockitoBean
    private lateinit var authService: AuthService

    @Test
    fun `회원가입 요청은 이메일과 비밀번호, 복구 이메일을 그대로 서비스로 전달한다`() {
        val body = mapOf(
            "email" to "user@example.com",
            "password" to "password1!",
            "recoveryEmail" to "recovery@example.com",
        )

        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)

        verify(authService).signUp("user@example.com", "password1!", "recovery@example.com")
    }

    @Test
    fun `이메일 중복 확인 요청은 서비스 결과를 duplicated 플래그로 응답한다`() {
        `when`(authService.checkEmailDuplicate("user@example.com")).thenReturn(true)

        mockMvc.perform(get("/auth/email/exists").param("email", "user@example.com"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.duplicated").value(true))
    }
}
