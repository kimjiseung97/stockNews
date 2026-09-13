package org.kjs.stocknews.common

// 외부 API(네이버 기업개요/뉴스 검색 등) 응답 텍스트에 섞여 들어오는 HTML 태그와
// 엔티티를 제거해 순수 텍스트로 정리하는 유틸.
// 주의: XSS 새니타이저가 아니다(엔티티 디코딩 결과로 "<script>" 같은 문자열이 남을 수 있음).
// 출력 시점의 이스케이프(React 기본 escape, 메일 템플릿 escape)는 별도로 지켜야 한다.
object HtmlTextUtils {

    // <br>, </p> 처럼 줄바꿈 의미를 가지는 태그는 개행으로 바꾼 뒤 나머지 태그를 제거한다.
    // 태그명 뒤 경계를 강제하지 않으면 <price>, <link>, <header> 같은 태그가 각각 p/li/hr로 오인돼
    // 엉뚱한 개행이 들어가므로 (?=[\s/>]) lookahead 로 태그명이 거기서 끝나는 경우만 잡는다.
    private val LINE_BREAK_TAG_REGEX = Regex("""<\s*/?\s*(?:br|p|div|li|tr|hr)(?=[\s/>])[^>]*>""", RegexOption.IGNORE_CASE)
    private val HTML_TAG_REGEX = Regex("""<[^>]*>""")
    private val NUMERIC_ENTITY_REGEX = Regex("""&#(x[0-9a-fA-F]+|[0-9]+);""")
    private val SPACE_RUN_REGEX = Regex("""[ \t ]+""")
    private val BLANK_LINE_RUN_REGEX = Regex("""\n{3,}""")

    private val NAMED_ENTITIES = listOf(
        "&nbsp;" to " ",
        "&quot;" to "\"",
        "&apos;" to "'",
        "&lt;" to "<",
        "&gt;" to ">",
        "&middot;" to "·",
        "&hellip;" to "…",
        "&amp;" to "&", // &amp;lt; 같은 이중 인코딩이 태그로 되살아나지 않도록 마지막에 치환한다.
    )

    // HTML 태그/엔티티를 제거하고 공백을 정리한 텍스트를 돌려준다.
    fun stripHtml(text: String): String {
        val withoutTags = text
            .replace(LINE_BREAK_TAG_REGEX, "\n")
            .replace(HTML_TAG_REGEX, "")

        // 디코딩은 한 번만 한다 - 디코딩 결과를 다시 태그로 취급하면 "5 &lt; 10 &gt; 3" 같은
        // 평문이 통째로 지워지므로, 이중 인코딩된 값은 텍스트로 남겨둔다.
        return unescapeEntities(withoutTags).normalizeWhitespace()
    }

    // null 이거나 정리 후 내용이 비면 null 을 돌려준다(공백/엔티티뿐인 값은 저장하지 않기 위함).
    fun stripHtmlOrNull(text: String?): String? =
        text?.let { stripHtml(it) }?.ifBlank { null }

    private fun unescapeEntities(text: String): String {
        val decodedNumeric = text.replace(NUMERIC_ENTITY_REGEX) { match ->
            val raw = match.groupValues[1]
            val code = if (raw.startsWith("x", ignoreCase = true)) {
                raw.drop(1).toIntOrNull(16)
            } else {
                raw.toIntOrNull()
            }
            // 서로게이트 영역(0xD800~0xDFFF)은 짝 없이 들어가면 DB/JSON 인코딩 단계에서 깨지므로 원문 그대로 남긴다.
            val isValidCodePoint = code != null && code in 1..0x10FFFF && code !in 0xD800..0xDFFF
            if (isValidCodePoint) {
                String(Character.toChars(code))
            } else {
                match.value
            }
        }

        return NAMED_ENTITIES.fold(decodedNumeric) { acc, (entity, replacement) ->
            acc.replace(entity, replacement, ignoreCase = true)
        }
    }

    // 줄 단위로 공백을 정리하고, 3줄 이상 연속된 개행은 빈 줄 하나로 줄인다.
    private fun String.normalizeWhitespace(): String =
        replace("\r\n", "\n")
            .replace('\r', '\n')
            .split("\n")
            .joinToString("\n") { it.replace(SPACE_RUN_REGEX, " ").trim() }
            .replace(BLANK_LINE_RUN_REGEX, "\n\n")
            .trim()
}
