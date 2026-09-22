package org.kjs.stocknews.service

// LlmClient 구현체가 던지는 실패의 공통 타입. 호출 측은 이 타입만 잡고 BusinessException(STOCK_CHAT_FAILED)로
// 바꾼다 - 제공자별 예외(NvidiaChatException 등)는 전부 이 클래스를 상속해야 한다.
open class LlmException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
