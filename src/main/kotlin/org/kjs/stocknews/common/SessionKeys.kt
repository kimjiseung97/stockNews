package org.kjs.stocknews.common

import jakarta.servlet.http.HttpSession

object SessionKeys {
    const val USER_ID = "USER_ID"
}

fun HttpSession.currentUserId(): Long =
    getAttribute(SessionKeys.USER_ID) as? Long ?: throw CustomException(ResultCode.UNAUTHORIZED)
