package org.kjs.stocknews.service

import org.slf4j.LoggerFactory

// 어드민이 DB(TB_PROMPT.CONTENT)에 저장해둔 프롬프트 본문에 런타임 값(오늘 날짜, 조회된 뉴스 등)을 끼워 넣는 템플릿 엔진.
// 문법은 두 가지뿐이다.
//   {{name}}          - 변수 치환. 값이 없으면 빈 문자열.
//   {{#name}}...{{/name}} - 조건부 구간. name 값이 비어있지 않을 때만 내용이 남는다.
//                            (예: 뉴스를 못 찾은 질문에 "아래는 최신 뉴스다" 문구가 붙는 걸 막는 용도)
// 어드민이 오타를 내 정의되지 않은 변수를 쓰면 조용히 지우지 않고 원문을 그대로 둔 채 경고 로그를 남긴다
// - 프롬프트 일부가 소리 없이 사라지는 것보다 눈에 띄는 편이 낫다.
object PromptTemplate {
    private val log = LoggerFactory.getLogger(PromptTemplate::class.java)

    private val SECTION_PATTERN = Regex("""\{\{#(\w+)}}(.*?)\{\{/\1}}""", RegexOption.DOT_MATCHES_ALL)
    private val VARIABLE_PATTERN = Regex("""\{\{(\w+)}}""")

    fun render(template: String, variables: Map<String, String?>): String {
        val sectionsApplied = SECTION_PATTERN.replace(template) { match ->
            val name = match.groupValues[1]
            if (variables[name].isNullOrBlank()) {
                ""
            } else {
                match.groupValues[2]
            }
        }
        return VARIABLE_PATTERN.replace(sectionsApplied) { match ->
            val name = match.groupValues[1]
            if (!variables.containsKey(name)) {
                log.warn("프롬프트 템플릿에 정의되지 않은 변수가 있습니다: {{{{{}}}}}", name)
                match.value
            } else {
                variables[name].orEmpty()
            }
        }
    }
}
