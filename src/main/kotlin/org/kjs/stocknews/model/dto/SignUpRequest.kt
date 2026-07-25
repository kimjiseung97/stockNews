package org.kjs.stocknews.model.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SignUpRequest(
    @field:NotBlank(message = "이메일을 입력해주세요")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    @field:Size(max = 50, message = "이메일은 50자 이하로 입력해주세요")
    val email: String,

    @field:NotBlank(message = "비밀번호를 입력해주세요")
    @field:Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해주세요")
    val password: String,
)
