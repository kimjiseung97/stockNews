package org.kjs.stocknews.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpSession
import org.kjs.stocknews.common.currentUserId
import org.kjs.stocknews.model.dto.NewsMailTestResponse
import org.kjs.stocknews.service.NewsMailTestService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// /users/me/** 는 WebConfig의 AuthInterceptor가 세션을 강제하므로 별도 인증 처리가 필요 없다.
@Tag(name = "NewsMailTest", description = "로그인한 사용자 본인에게 뉴스 다이제스트 메일을 즉시 테스트 발송하는 API")
@RestController
@RequestMapping("/users/me/news-mail")
class NewsMailTestController(
    private val newsMailTestService: NewsMailTestService,
) {
    @Operation(
        summary = "뉴스 다이제스트 테스트 발송",
        description = "발송시간대/발송여부 설정과 무관하게, 현재 로그인한 사용자의 관심종목 뉴스를 본인 이메일로 즉시 발송한다. 남용 방지를 위해 1분에 한 번만 호출할 수 있다.",
    )
    @PostMapping("/test")
    fun sendTest(session: HttpSession): NewsMailTestResponse =
        newsMailTestService.sendTestDigest(session.currentUserId())
}
