package org.kjs.stocknews.service

// NVIDIA NIM API 호출/응답 처리 실패를 나타내는 예외. 원인(상태코드/timeout/파싱 실패 등)을
// message에 담아 상위 계층(StockChatService -> GlobalExceptionHandler)까지 한 번만 로깅되게 한다.
open class NvidiaChatException(message: String, cause: Throwable? = null) : LlmException(message, cause)

// 그 모델만의 문제로 호출이 실패한 경우 - 단종(410), 계정에 호출 권한 없음(404), 추론 백엔드 장애(5xx).
// NvidiaChatClient가 이 타입만 잡아 modelsToTry의 다음 모델로 자동 전환한다.
class NvidiaModelUnavailableException(message: String, cause: Throwable? = null) : NvidiaChatException(message, cause)
