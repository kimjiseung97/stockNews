package org.kjs.stocknews.model.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// 요청(system/user)과 응답(assistant) 양쪽에 쓰는 메시지.
//
// 추론(thinking) 모델은 생각을 reasoning_content에, 답변을 content에 나눠 담는다. 토큰 예산(max_tokens)을
// 생각에 다 써버리면 content가 null로 온다(2026-09-22 z-ai/glm-5.3-flash 실측 - 94초 뒤 content=null).
// content를 non-null로 두면 그 순간 JSON 파싱 자체가 죽어 "무엇이 왔는지"가 로그에 남지 않으므로
// nullable로 받고 판단은 NvidiaChatClient가 한다.
//
// NON_NULL은 reasoningContent 필드에만 건다. 클래스 단위로 걸면 content=null인 요청이 만들어졌을 때
// "content": null도 아니고 키가 통째로 빠진 요청이 조용히 나가버린다 - 요청의 content는 항상 실려야 한다.
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiChatMessage(
    val role: String,
    val content: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("reasoning_content")
    val reasoningContent: String? = null,
)
