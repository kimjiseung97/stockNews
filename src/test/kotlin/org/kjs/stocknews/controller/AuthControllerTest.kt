package org.kjs.stocknews.controller

import org.junit.jupiter.api.Test
import org.kjs.stocknews.service.AuthService
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
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
    fun `비밀번호가 최대 길이를 초과하면 회원가입 요청이 거부된다`() {
        val tooLongPassword = "a".repeat(21)
        val body = mapOf("email" to "user@example.com", "password" to tooLongPassword)

        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        verify(authService, never()).signUp(anyString(), anyString())
    }

    @Test
    fun `이메일이 최대 길이를 초과하면 회원가입 요청이 거부된다`() {
        val tooLongEmail = "a".repeat(45) + "@test.com"
        val body = mapOf("email" to tooLongEmail, "password" to "password1!")

        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        verify(authService, never()).signUp(anyString(), anyString())
    }

    @Test
    fun `유효한 길이의 이메일과 비밀번호는 회원가입 서비스로 전달된다`() {
        val body = mapOf("email" to "user@example.com", "password" to "password1!")

        mockMvc.perform(
            post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)

        verify(authService).signUp("user@example.com", "password1!")
    }

    @Test
    fun `비밀번호가 최소 길이 미만이면 로그인 요청이 거부된다`() {
        val body = mapOf("email" to "user@example.com", "password" to "short1!")

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))

        verify(authService, never()).login(anyString(), anyString())
    }
}
