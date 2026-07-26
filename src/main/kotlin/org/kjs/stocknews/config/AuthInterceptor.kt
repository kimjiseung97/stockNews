package org.kjs.stocknews.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.common.SessionKeys
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthInterceptor : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.getSession(false)?.getAttribute(SessionKeys.USER_ID) as? Long
            ?: throw BusinessException(ResultCode.UNAUTHORIZED)
        return true
    }
}
