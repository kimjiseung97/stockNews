package org.kjs.stocknews.common

class BusinessException(
    val resultCode: ResultCode,
    message: String = resultCode.message,
) : RuntimeException(message)
