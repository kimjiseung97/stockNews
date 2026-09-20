package org.kjs.stocknews.config

import org.springframework.ai.transformers.TransformersEmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy

// 챗봇 질의를 임베딩하는 로컬 ONNX 모델. 외부 임베딩 API를 쓰지 않는다.
//
// Spring AI의 자동설정(TransformersEmbeddingModelAutoConfiguration)을 쓰지 않고 직접 만든다.
// 자동설정이 만든 빈은 컨텍스트 기동 때 싱글턴으로 즉시 생성되는데, 이 모델은 수백 MB를 힙에
// 올리므로 기동만으로 OutOfMemoryError가 난다(테스트 JVM에서 실제로 발생했다).
// @Lazy로 두면 첫 챗봇 질문이 들어올 때 만들어지고, 컨텍스트만 띄우는 테스트는 건드리지 않는다.
// 자동설정은 spring.ai.model.embedding=none으로 꺼둔다.
//
// 모델은 파이썬 embedding-service의 적재 모델과 반드시 같아야 한다. 다르면 차원이 같아도
// 좌표계가 달라 검색 결과가 조용히 무의미해진다.
//
// 운영 주의: 첫 호출에서 모델 파일을 내려받아(캐시 디렉터리) 힙에 올린다. JVM 최대 힙이
// 모델 크기보다 넉넉해야 하며, 컨테이너 메모리 상한도 함께 봐야 한다.
@Configuration
class EmbeddingModelConfig {
    @Bean
    @Lazy
    fun embeddingModel(
        @Value("\${news.vector-search.model-uri}") modelUri: String,
        @Value("\${news.vector-search.tokenizer-uri}") tokenizerUri: String,
        // 매 기동마다 수백 MB를 다시 받지 않도록 캐시 경로를 고정한다.
        @Value("\${news.vector-search.model-cache-directory}") cacheDirectory: String,
    ): TransformersEmbeddingModel {
        val model = TransformersEmbeddingModel()
        model.setModelResource(modelUri)
        model.setTokenizerResource(tokenizerUri)
        model.setResourceCacheDirectory(cacheDirectory)
        model.setTokenizerOptions(
            mapOf(
                // 길이가 제각각인 입력을 한 배치로 넣을 때 "Supplied array is ragged" 오류를 막는다.
                "padding" to "true",
                // e5 계열 컨텍스트는 512 토큰이다. 넘는 입력은 잘라야 모델이 거부하지 않는다.
                "maxLength" to "512",
                "truncation" to "true",
            ),
        )
        // afterPropertiesSet()을 여기서 직접 부르지 않는다. TransformersEmbeddingModel은
        // InitializingBean이라 @Bean으로 돌려주면 Spring이 호출한다. 직접 부르면 수백 MB 모델을
        // 두 번 초기화하게 된다(문서의 수동 호출 안내는 컨테이너 밖에서 new 할 때의 이야기다).
        return model
    }
}
