package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.SecCompanyProfile
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Component
class SecCompanyProfileClient(
    @Value("\${sec.user-agent}") private val userAgent: String,
) {
    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
            ).apply { setReadTimeout(Duration.ofSeconds(15)) },
        )
        .build()

    fun fetchProfile(cik: Long): SecCompanyProfile? {
        val paddedCik = cik.toString().padStart(10, '0')
        return restClient.get()
            .uri("https://data.sec.gov/submissions/CIK$paddedCik.json")
            .header(HttpHeaders.USER_AGENT, userAgent)
            .retrieve()
            .body(SecCompanyProfile::class.java)
    }
}
