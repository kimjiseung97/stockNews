package org.kjs.stocknews.service

// 챗봇이 쓰는 LLM 호출의 추상화. 제공자(NVIDIA NIM, Anthropic, OpenAI ...)마다 구현체를 하나씩 두고
// llm.provider 설정으로 그중 하나만 빈으로 올린다. 호출 측(StockChatService)은 어느 제공자인지 모른다.
//
// 계약:
//  - systemPrompt는 변수 치환까지 끝난 완성된 시스템 프롬프트, userMessage는 사용자 질문 원문이다.
//  - 성공하면 비어 있지 않은 답변 본문을 돌려준다.
//  - 실패는 LlmException(또는 그 하위 타입)으로만 던진다. 재시도·모델 폴백처럼 제공자마다 다른 복구는
//    구현체 안에서 끝내고, 여기까지 올라온 예외는 "이 제공자로는 답을 못 받았다"는 뜻이다.
//  - 로깅은 여기서 하지 않는다. 진단에 필요한 정보(모델명, 상태코드, 타임아웃 값 등)는 예외 메시지에 담아
//    상위 계층(GlobalExceptionHandler)에서 한 번만 남기게 한다.
//
// 새 제공자를 붙이는 순서: 구현체를 만들고 @ConditionalOnProperty(prefix = "llm", name = "provider",
// havingValue = "<이름>")을 붙인 뒤, application-*.yml의 llm.provider를 그 이름으로 바꾼다.
interface LlmClient {
    fun chat(systemPrompt: String, userMessage: String): String
}
