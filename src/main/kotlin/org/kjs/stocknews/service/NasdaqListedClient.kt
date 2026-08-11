package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NasdaqListedEntry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

// nasdaqlisted.txt: Symbol|Security Name|Market Category|Test Issue|Financial Status|Round Lot Size|ETF|NextShares
private const val NASDAQ_TEST_ISSUE_INDEX = 3

// otherlisted.txt: ACT Symbol|Security Name|Exchange|CQS Symbol|ETF|Round Lot Size|Test Issue|NASDAQ Symbol
private const val OTHER_TEST_ISSUE_INDEX = 6

@Component
class NasdaqListedClient(
    @Value("\${nasdaq.listed-url}") private val nasdaqListedUrl: String,
    @Value("\${nasdaq.other-listed-url}") private val otherListedUrl: String,
) {
    private val log = LoggerFactory.getLogger(NasdaqListedClient::class.java)

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(30)) },
        )
        .build()

    // 나스닥 상장(nasdaqlisted.txt) + 그 외 거래소(NYSE/AMEX 등, otherlisted.txt) 상장 종목을 합쳐서 반환한다.
    // 테스트 발행물(Test Issue=Y)은 제외. 두 파일의 컬럼 순서가 서로 달라 개별 인덱스로 파싱한다.
    fun fetchAllListed(): List<NasdaqListedEntry> {
        val nasdaq = parse(fetchRaw(nasdaqListedUrl), testIssueIndex = NASDAQ_TEST_ISSUE_INDEX)
        val other = parse(fetchRaw(otherListedUrl), testIssueIndex = OTHER_TEST_ISSUE_INDEX)
        log.info("nasdaq trader fetched {} nasdaq-listed + {} other-listed entries", nasdaq.size, other.size)
        return nasdaq + other
    }

    private fun fetchRaw(url: String): String =
        restClient.get().uri(url).retrieve().body(String::class.java) ?: ""

    // 두 파일 모두 Symbol/ACT Symbol이 0번, Security Name이 1번 컬럼이라 심볼/이름 인덱스는 고정.
    // Test Issue 컬럼 위치만 파일마다 달라 파라미터로 받는다.
    private fun parse(raw: String, testIssueIndex: Int): List<NasdaqListedEntry> =
        raw.lineSequence()
            .drop(1)
            .filter { it.isNotBlank() && !it.startsWith("File Creation Time") }
            .mapNotNull { line ->
                val cols = line.split("|")
                if (cols.size <= testIssueIndex) return@mapNotNull null
                if (cols[testIssueIndex] == "Y") return@mapNotNull null
                val symbol = cols[0].trim()
                val name = cols[1].trim()
                if (symbol.isBlank()) null else NasdaqListedEntry(ticker = symbol, name = name)
            }
            .toList()
}
