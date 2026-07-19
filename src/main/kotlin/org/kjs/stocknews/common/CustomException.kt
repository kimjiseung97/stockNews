package org.kjs.stocknews.common

class CustomException(
    val resultCode: ResultCode,
    message: String = resultCode.message,
) : RuntimeException(message)
