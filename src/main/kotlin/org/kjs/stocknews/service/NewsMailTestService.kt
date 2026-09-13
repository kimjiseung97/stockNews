package org.kjs.stocknews.service

import jakarta.mail.MessagingException
import org.kjs.stocknews.common.BusinessException
import org.kjs.stocknews.common.ResultCode
import org.kjs.stocknews.model.dto.NewsArticle
import org.kjs.stocknews.model.dto.NewsMailTestResponse
import org.kjs.stocknews.model.table.User
import org.kjs.stocknews.repository.StockNewsRepository
import org.kjs.stocknews.repository.UserRepository
import org.kjs.stocknews.repository.UserStockRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.mail.MailException
import org.springframework.stereotype.Service
import java.io.UnsupportedEncodingException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

// 연타로 SMTP를 두들기지 못하도록 유저당 최소 재시도 간격을 둔다(네이버 SMTP 발송량 제한 보호).
private val TEST_DISPATCH_COOLDOWN: Duration = Duration.ofMinutes(1)

// 뉴스 다이제스트 메일이 실제로 어떻게 오는지 즉시 확인하기 위한 테스트 발송.
// 운영 배치(NewsDispatchJobConfig)와 달리 발송시간대/발송여부 설정을 보지 않고, 호출한 본인에게만 바로 보낸다.
// 메일 본문 구성은 운영 발송과 같은 NewsMailSender를 그대로 써서 실제 발송물과 동일하게 나오도록 한다.
@Service
class NewsMailTestService(
    private val userRepository: UserRepository,
    private val userStockRepository: UserStockRepository,
    private val stockNewsRepository: StockNewsRepository,
    private val newsMailSender: NewsMailSender,
    @Value("\${news.dispatch.max-articles-per-stock:1}") private val maxArticlesPerStock: Int,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val log = LoggerFactory.getLogger(NewsMailTestService::class.java)

    // 단일 인스턴스(docker compose app 1개) 운영이라 in-memory로 충분하다.
    // 스케일아웃하거나 재시작 간에도 유지해야 한다면 DB/Redis 기반 제한으로 옮겨야 한다.
    private val lastSentAtByUserId = ConcurrentHashMap<Long, Instant>()

    fun sendTestDigest(userId: Long): NewsMailTestResponse {
        val now = Instant.now(clock)
        // 조회 후 저장 사이에 동시 요청이 끼어들면 둘 다 통과해 메일이 중복 발송되므로,
        // 검사와 갱신을 compute로 한 번에(원자적으로) 처리해 슬롯을 선점한다.
        reserveCooldownSlot(userId, now)

        val user: User
        val articlesByTicker: Map<String, List<NewsArticle>>
        try {
            user = userRepository.findById(userId).orElseThrow { BusinessException(ResultCode.UNAUTHORIZED) }
            articlesByTicker = collectArticles(userId)
        } catch (e: Exception) {
            // 메일을 보내보지도 못하고 실패한 경우는 쿨다운을 소모시키지 않는다
            // (관심종목을 등록하고 바로 다시 시도할 수 있어야 한다).
            releaseCooldownSlot(userId, now)
            throw e
        }

        // 반대로 SMTP 발송 실패는 쿨다운을 유지한다 - 실패해도 연타로 메일 서버를 두들기는 건 똑같이 막아야 한다.
        try {
            newsMailSender.sendNewsDigest(user.email, articlesByTicker)
        } catch (e: Exception) {
            // MimeMessageHelper 조립 단계에서 나오는 MessagingException/UnsupportedEncodingException은
            // MailException이 아니라서 함께 잡아주지 않으면 INTERNAL_ERROR로 뭉개진다.
            when (e) {
                is MailException, is MessagingException, is UnsupportedEncodingException ->
                    throw BusinessException(ResultCode.NEWS_MAIL_SEND_FAILED, cause = e)
                else -> throw e
            }
        }

        val articleCount = articlesByTicker.values.sumOf { it.size }
        log.info("test news digest sent userId={} stockCount={} articleCount={}", userId, articlesByTicker.size, articleCount)
        return NewsMailTestResponse(email = user.email, stockCount = articlesByTicker.size, articleCount = articleCount)
    }

    // 선점 성공 여부를 반환값(Instant) 비교로 판단하면 두 요청의 시각이 같을 때 둘 다 통과해버린다.
    // compute 람다는 해당 키에 대해 원자적으로 실행되므로, 선점 판정 자체를 람다 안에서 내린다.
    private fun reserveCooldownSlot(userId: Long, now: Instant) {
        var reserved = false
        lastSentAtByUserId.compute(userId) { _, previous ->
            if (previous != null && Duration.between(previous, now) < TEST_DISPATCH_COOLDOWN) {
                previous
            } else {
                reserved = true
                now
            }
        }
        if (!reserved) {
            throw BusinessException(ResultCode.NEWS_MAIL_TEST_TOO_FREQUENT)
        }
    }

    // 내가 선점한 슬롯일 때만 되돌린다(그 사이 다른 요청이 선점했다면 건드리지 않는다).
    private fun releaseCooldownSlot(userId: Long, reservedAt: Instant) {
        lastSentAtByUserId.remove(userId, reservedAt)
    }

    private fun collectArticles(userId: Long): Map<String, List<NewsArticle>> {
        val stockViews = userStockRepository.findNewsViewsByUserIdIn(listOf(userId))
        if (stockViews.isEmpty()) {
            throw BusinessException(ResultCode.NEWS_MAIL_NO_STOCKS)
        }

        // 설정값이 0 이하로 들어오면 PageRequest.of가 IllegalArgumentException을 던지므로 최소 1건은 보장한다.
        val pageable = PageRequest.of(0, maxArticlesPerStock.coerceAtLeast(1))
        val articlesByTicker = linkedMapOf<String, List<NewsArticle>>()
        for (stock in stockViews) {
            val articles = stockNewsRepository.findByStockIdOrderByCollectedAtDesc(stock.stockId, pageable)
                .content
                .map { NewsArticle(title = it.title, url = it.url, description = it.content) }
            if (articles.isNotEmpty()) {
                articlesByTicker.put(stock.ticker, articles)
            }
        }
        if (articlesByTicker.isEmpty()) {
            throw BusinessException(ResultCode.NEWS_MAIL_NO_ARTICLES)
        }
        return articlesByTicker
    }
}
