package org.kjs.stocknews.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import org.kjs.stocknews.common.currentUserId
import org.kjs.stocknews.model.dto.MailSendSettingRequest
import org.kjs.stocknews.model.dto.MailSendSettingResponse
import org.kjs.stocknews.service.UserMailSendSettingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "UserMailSendSetting", description = "로그인한 사용자의 뉴스 다이제스트 메일 발송 여부 설정 API")
@RestController
@RequestMapping("/users/me/mail-send-setting")
class UserMailSendSettingController(
    private val userMailSendSettingService: UserMailSendSettingService,
) {
    @Operation(summary = "메일 발송 설정 조회", description = "현재 로그인한 사용자의 뉴스 다이제스트 메일 발송 여부를 조회한다. 설정한 적 없으면 발송(true)이 기본값이다.")
    @GetMapping
    fun get(session: HttpSession): MailSendSettingResponse =
        userMailSendSettingService.get(session.currentUserId())

    @Operation(summary = "메일 발송 설정 등록/변경", description = "현재 로그인한 사용자의 뉴스 다이제스트 메일 발송 여부를 등록하거나 변경한다.")
    @PostMapping
    fun register(@RequestBody request: MailSendSettingRequest, session: HttpSession): MailSendSettingResponse =
        userMailSendSettingService.register(session.currentUserId(), request.mailEnabled)
}
