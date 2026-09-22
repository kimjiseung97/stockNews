package org.kjs.stocknews.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

// llm.provider 값에 따라 LlmClient 구현체가 정확히 하나만 뜨는지 고정한다. 세 구현체가 동시에 뜨면
// StockChatService 주입이 모호해지고, 하나도 안 뜨면 기동이 실패한다.
class LlmClientSelectionTest {
    private val runner = ApplicationContextRunner()
        .withUserConfiguration(NvidiaChatClient::class.java, ClaudeChatClient::class.java, GptClient::class.java)
        .withPropertyValues(
            "nvidia.api.base-url=http://127.0.0.1:1/v1",
            "nvidia.api.key=k",
            "nvidia.api.model=m",
            "anthropic.api.key=k",
            "openai.api.key=k",
        )

    private fun assertOnly(expected: Class<out LlmClient>, vararg properties: String) {
        runner.withPropertyValues(*properties).run { context ->
            val beans = context.getBeansOfType(LlmClient::class.java)
            assertEquals(1, beans.size, "LlmClient 빈은 정확히 하나여야 한다: ${beans.keys}")
            assertTrue(expected.isInstance(beans.values.first()), "expected ${expected.simpleName}, got ${beans.values.first()::class.simpleName}")
        }
    }

    @Test
    fun `provider가 없으면 nvidia가 뜬다`() = assertOnly(NvidiaChatClient::class.java)

    @Test
    fun `provider=nvidia`() = assertOnly(NvidiaChatClient::class.java, "llm.provider=nvidia")

    @Test
    fun `provider=anthropic`() = assertOnly(ClaudeChatClient::class.java, "llm.provider=anthropic")

    @Test
    fun `provider=openai`() = assertOnly(GptClient::class.java, "llm.provider=openai")

    // 빈 문자열은 '없음'이 아니다. 이 경우 어떤 구현체도 뜨지 않아 실제 앱은 기동에 실패한다 -
    // 그래서 배포 스크립트가 빈 시크릿을 nvidia로 채운다(deploy.yml). 이 동작이 바뀌면 여기서 드러나게 둔다.
    @Test
    fun `provider가 빈 문자열이면 어떤 구현체도 뜨지 않는다`() {
        runner.withPropertyValues("llm.provider=").run { context ->
            assertEquals(0, context.getBeansOfType(LlmClient::class.java).size)
        }
    }
}
