package org.kjs.stocknews.common

data class ApiResponse<out T>(
    val code: String,
    val message: String,
    val data: T?,
) {
    companion object {
        fun <T> success(data: T? = null, message: String = ResultCode.OK.message): ApiResponse<T> =
            ApiResponse(code = ResultCode.OK.code, message = message, data = data)

        fun fail(resultCode: ResultCode): ApiResponse<Nothing> =
            ApiResponse(code = resultCode.code, message = resultCode.message, data = null)
    }
}
