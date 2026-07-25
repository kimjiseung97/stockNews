package org.kjs.stocknews.model.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class VerifyEmailRequest(
    @field:NotBlank(message = "이메일을 입력해주세요")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:Size(max = 50, message = "이메일은 50자 이하로 입력해주세요")
    val email: String,

    @field:NotBlank(message = "인증코드를 입력해주세요")
    @field:Size(min = 6, max = 6, message = "인증코드는 6자리입니다")
    val code: String,
)
