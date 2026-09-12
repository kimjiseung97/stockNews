package org.kjs.stocknews.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class HtmlTextUtilsTest {

    @Test
    @DisplayName("br 태그는 개행으로 바뀐다")
    fun convertsBrTagToNewline() {
        assertEquals("첫 줄\n둘째 줄", HtmlTextUtils.stripHtml("첫 줄<br>둘째 줄"))
        assertEquals("첫 줄\n둘째 줄", HtmlTextUtils.stripHtml("첫 줄<br/>둘째 줄"))
        assertEquals("첫 줄\n둘째 줄", HtmlTextUtils.stripHtml("첫 줄<BR />둘째 줄"))
    }

    @Test
    @DisplayName("일반 태그는 내용만 남기고 제거된다")
    fun removesInlineTags() {
        assertEquals(
            "애플은 스마트폰을 만든다",
            HtmlTextUtils.stripHtml("""<p><b>애플</b>은 <span class="x">스마트폰</span>을 만든다</p>"""),
        )
    }

    @Test
    @DisplayName("HTML 엔티티가 디코딩된다")
    fun decodesEntities() {
        assertEquals(
            "\"AT&T\" 's 5 < 10 … 공백",
            HtmlTextUtils.stripHtml("&quot;AT&amp;T&quot; &#39;s 5 &lt; 10 &hellip;&nbsp;공백"),
        )
    }

    @Test
    @DisplayName("엔티티 디코딩은 한 번만 하고 부등호 평문은 보존한다")
    fun decodesEntitiesOnlyOnce() {
        // 이중 인코딩 값은 한 단계만 풀려 텍스트로 남는다(태그로 되살아나 잘리지 않는다).
        assertEquals("&lt;b&gt;굵게&lt;/b&gt;", HtmlTextUtils.stripHtml("&amp;lt;b&amp;gt;굵게&amp;lt;/b&amp;gt;"))
        assertEquals("5 < 10 > 3", HtmlTextUtils.stripHtml("5 &lt; 10 &gt; 3"))
    }

    @Test
    @DisplayName("연속 공백과 빈 줄이 정리된다")
    fun normalizesWhitespace() {
        assertEquals("한 줄\n\n다음 문단", HtmlTextUtils.stripHtml("  한    줄 <br><br><br><br> 다음 문단  "))
    }

    @Test
    @DisplayName("태그가 없는 평문은 그대로 유지된다")
    fun keepsPlainText() {
        assertEquals("반도체와 소프트웨어", HtmlTextUtils.stripHtml("반도체와 소프트웨어"))
    }

    @Test
    @DisplayName("stripHtmlOrNull은 null·공백 결과를 null로 돌려준다")
    fun returnsNullForBlank() {
        assertNull(HtmlTextUtils.stripHtmlOrNull(null))
        assertNull(HtmlTextUtils.stripHtmlOrNull("   "))
        assertNull(HtmlTextUtils.stripHtmlOrNull("<br><p>&nbsp;</p>"))
        assertEquals("내용", HtmlTextUtils.stripHtmlOrNull("<p>내용</p>"))
    }
}
