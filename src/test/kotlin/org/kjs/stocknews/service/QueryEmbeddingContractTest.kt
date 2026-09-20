package org.kjs.stocknews.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.kjs.stocknews.config.EmbeddingModelConfig
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import kotlin.math.sqrt

// Boot가 만든 질의 벡터가 파이썬 embedding-service가 적재에 쓴 벡터와 같은 좌표계에 있는지 확인한다.
//
// 이게 왜 필요한가: 모델·풀링·양자화 중 하나만 달라도 에러 없이 "검색 결과가 이상한" 상태가 된다.
// 차원이 같으면 쿼리는 성공하고 점수도 나오므로 로그만 봐서는 절대 못 잡는다. 유일하게 확실한
// 확인 방법은 같은 문장을 양쪽에서 임베딩해 코사인 유사도를 재보는 것이다.
//
// 기본 실행에서는 제외한다 - 수백 MB 모델 다운로드와 파이썬 서비스 기동이 필요하다.
// 실행 방법:
//   1) embedding-service를 띄운다(기본 http://localhost:8000)
//   2) EMBEDDING_CONTRACT_TEST=true ./gradlew test --tests "*QueryEmbeddingContractTest"
@EnabledIfEnvironmentVariable(named = "EMBEDDING_CONTRACT_TEST", matches = "true")
class QueryEmbeddingContractTest {
    private val pythonBaseUrl = System.getenv("EMBEDDING_BASE_URL") ?: "http://localhost:8000"

    private val restClient = RestClient.builder()
        .requestFactory(
            // HTTP/1.1을 명시한다. JDK 기본값(HTTP/2)은 평문 http에서 h2c 업그레이드를 시도하는데,
            // uvicorn이 그 헤더를 보면 바디를 애플리케이션에 넘기지 않아 422가 돌아온다.
            JdkClientHttpRequestFactory(HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()),
        )
        .build()

    private val embeddingModel = EmbeddingModelConfig().embeddingModel(
        modelUri = System.getenv("EMBEDDING_ONNX_MODEL_URI")
            ?: "https://huggingface.co/intfloat/multilingual-e5-large/resolve/main/onnx/model_qint8_avx512_vnni.onnx",
        tokenizerUri = System.getenv("EMBEDDING_ONNX_TOKENIZER_URI")
            ?: "https://huggingface.co/intfloat/multilingual-e5-large/resolve/main/onnx/tokenizer.json",
        cacheDirectory = System.getProperty("java.io.tmpdir") + "/spring-ai-onnx-model",
    )

    @Test
    fun `Boot의 질의 벡터가 파이썬과 같은 차원이다`() {
        val vector = embeddingModel.embed("query: 애플 실적 어때?")

        // 적재 테이블 news_chunks.embedding이 vector(1024)다. 다르면 조회 자체가 실패한다.
        assertEquals(1024, vector.size)
    }

    @Test
    fun `Boot의 질의 벡터가 파이썬의 질의 벡터와 사실상 같다`() {
        val sentence = "애플 실적 어때?"

        val bootVector = embeddingModel.embed("query: $sentence")
        val pythonVector = pythonEmbed(sentence, kind = "query")

        val similarity = cosineSimilarity(bootVector, pythonVector)
        // 같은 모델이라면 양자화 오차만 남아 0.99 위여야 한다. 이보다 낮으면 풀링 방식이나
        // 토크나이저가 다른 것이고, 그대로 두면 검색 품질이 조용히 무너진다.
        assertTrue(similarity > 0.99, "코사인 유사도가 너무 낮다: $similarity")
    }

    @Test
    fun `Boot의 질의 벡터가 파이썬 passage 벡터와도 정렬된다`() {
        // 실제 검색은 query 벡터와 passage 벡터를 비교한다. 의미가 같은 문장끼리
        // 충분히 가까운지까지 봐야 파이프라인 전체가 맞물렸다고 할 수 있다.
        val bootQuery = embeddingModel.embed("query: 애플 실적")
        val pythonPassage = pythonEmbed("애플이 분기 실적을 발표했다.", kind = "passage")

        val similarity = cosineSimilarity(bootQuery, pythonPassage)
        // 서로 다른 문장이라 1에 가깝지는 않다. min-score 기본값(0.80)을 넘는지를 본다.
        assertTrue(similarity > 0.80, "의미가 같은 문장인데 유사도가 낮다: $similarity")
    }

    private fun pythonEmbed(text: String, kind: String): FloatArray {
        val response = restClient.post()
            .uri("$pythonBaseUrl/v1/embed")
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("texts" to listOf(text), "kind" to kind))
            .retrieve()
            .body(Map::class.java)

        @Suppress("UNCHECKED_CAST")
        val embeddings = response!!["embeddings"] as List<List<Number>>
        return embeddings.first().map { it.toFloat() }.toFloatArray()
    }

    private fun cosineSimilarity(left: FloatArray, right: FloatArray): Double {
        assertEquals(left.size, right.size, "차원이 다르면 비교 자체가 무의미하다")
        var dot = 0.0
        var leftNorm = 0.0
        var rightNorm = 0.0
        for (index in left.indices) {
            dot += left[index].toDouble() * right[index].toDouble()
            leftNorm += left[index].toDouble() * left[index].toDouble()
            rightNorm += right[index].toDouble() * right[index].toDouble()
        }
        return dot / (sqrt(leftNorm) * sqrt(rightNorm))
    }
}
