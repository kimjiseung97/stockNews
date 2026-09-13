package org.kjs.stocknews.service

import org.kjs.stocknews.repository.PromptRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

// 어드민이 DB에 저장해둔 프롬프트를 가져와 런타임 값과 조합해주는 서비스.
//
// 조회 흐름: 캐시 -> TB_PROMPT(CODE + ENABLED=true) -> (없거나 DB 실패 시) 직전 캐시 -> 코드 내 fallback
// 프롬프트는 채팅 요청마다 필요한데 내용은 어드민이 가끔 고칠 뿐이라, 매 요청 DB를 때리지 않도록
// 짧은 TTL(기본 60초) 캐시를 둔다. 어드민 수정은 길어야 TTL 만큼 뒤에 반영된다.
@Service
class PromptService(
    private val promptRepository: PromptRepository,
    @Value("\${prompt.cache-ttl-seconds:60}") cacheTtlSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(PromptService::class.java)
    private val cacheTtl: Duration = Duration.ofSeconds(cacheTtlSeconds)
    private val cache = ConcurrentHashMap<PromptCode, CachedPrompt>()

    // 프롬프트 본문을 찾아 변수를 끼워 넣은 최종 문자열을 돌려준다.
    fun render(promptCode: PromptCode, variables: Map<String, String?>): String =
        PromptTemplate.render(contentOf(promptCode), variables)

    // 어드민이 프롬프트를 고친 직후 바로 확인하고 싶을 때를 위한 캐시 무효화.
    fun evictCache() {
        cache.clear()
    }

    // compute로 감싸 같은 코드에 대한 갱신을 직렬화한다 - TTL이 끝나는 순간 동시 요청이 몰려도 DB 조회는 한 번만 나간다.
    private fun contentOf(promptCode: PromptCode): String =
        cache.compute(promptCode) { code, cached ->
            if (cached != null && !cached.isExpired()) cached else CachedPrompt(load(code, cached), Instant.now().plus(cacheTtl))
        }!!.content

    private fun load(promptCode: PromptCode, cached: CachedPrompt?): String {
        try {
            val found = promptRepository.findByCodeAndEnabledIsTrue(promptCode.code)
            // 조회에 성공했는데 행이 없다면 어드민이 일부러 지웠거나 꺼둔 것이다. 이때 캐시를 재사용하면
            // 문제가 생긴 프롬프트를 어드민이 꺼도 계속 쓰게 되므로, 캐시를 버리고 코드 기본값으로 내려간다.
            if (found == null) {
                log.warn("활성 프롬프트가 없어 기본 프롬프트를 사용합니다: {}", promptCode.code)
                return promptCode.fallback
            }
            return found.content
        } catch (e: DataAccessException) {
            // DB가 흔들려도 챗봇은 답해야 한다 - 만료된 캐시라도 있으면 그걸 쓰고, 없으면 코드 기본값으로 간다.
            log.warn("프롬프트 조회 실패({}) - 캐시/기본 프롬프트로 대체합니다.", promptCode.code, e)
            return cached?.content ?: promptCode.fallback
        }
    }

    private class CachedPrompt(val content: String, private val expiresAt: Instant) {
        fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    }
}
