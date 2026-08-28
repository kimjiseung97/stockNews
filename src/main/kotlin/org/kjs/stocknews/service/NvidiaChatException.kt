package org.kjs.stocknews.service

// NVIDIA NIM API 호출/응답 처리 실패를 나타내는 예외. 원인(상태코드/timeout/파싱 실패 등)을
// message에 담아 상위 계층(StockChatService -> GlobalExceptionHandler)까지 한 번만 로깅되게 한다.
class NvidiaChatException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
