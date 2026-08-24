package org.kjs.stocknews.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import org.kjs.stocknews.common.currentUserId
import org.kjs.stocknews.model.dto.MailDispatchSettingRequest
import org.kjs.stocknews.model.dto.MailDispatchSettingResponse
import org.kjs.stocknews.service.UserMailDispatchSettingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "UserMailDispatchSetting", description = "로그인한 사용자의 뉴스 다이제스트 메일 발송시간대 설정 API")
@RestController
@RequestMapping("/users/me/mail-dispatch-setting")
class UserMailDispatchSettingController(
    private val userMailDispatchSettingService: UserMailDispatchSettingService,
) {
    @Operation(summary = "메일 발송시간대 조회", description = "현재 로그인한 사용자의 뉴스 다이제스트 메일 발송시간대를 조회한다. 설정한 적 없으면 09:00이 기본값이다.")
    @GetMapping
    fun get(session: HttpSession): MailDispatchSettingResponse =
        userMailDispatchSettingService.get(session.currentUserId())

    @Operation(summary = "메일 발송시간대 등록/변경", description = "현재 로그인한 사용자의 뉴스 다이제스트 메일 발송시간대를 등록하거나 변경한다. 30분 단위(예: 09:00, 09:30)만 허용한다.")
    @PostMapping
    fun register(@RequestBody request: MailDispatchSettingRequest, session: HttpSession): MailDispatchSettingResponse =
        userMailDispatchSettingService.register(session.currentUserId(), request.dispatchTime)
}
