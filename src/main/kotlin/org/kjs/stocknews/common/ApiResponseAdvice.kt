package org.kjs.stocknews.common

import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import tools.jackson.databind.ObjectMapper

@RestControllerAdvice
class ApiResponseAdvice(
    private val objectMapper: ObjectMapper,
) : ResponseBodyAdvice<Any> {

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean =
        returnType.parameterType != ApiResponse::class.java

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        if (body is ApiResponse<*>) return body

        val wrapped = ApiResponse.success(body)

        // String-returning endpoints are written by StringHttpMessageConverter, which cannot serialize ApiResponse directly.
        if (returnType.parameterType == String::class.java) {
            response.headers.contentType = MediaType.APPLICATION_JSON
            return objectMapper.writeValueAsString(wrapped)
        }
        return wrapped
    }
}
