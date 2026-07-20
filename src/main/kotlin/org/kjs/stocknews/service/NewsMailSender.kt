package org.kjs.stocknews.service

import org.kjs.stocknews.model.dto.NewsArticle
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class NewsMailSender(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username}") private val fromAddress: String,
    @Value("\${mail.from-name}") private val fromName: String,
) {
    fun sendNewsDigest(email: String, articlesByTicker: Map<String, List<NewsArticle>>) {
        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage)
        helper.setFrom(fromAddress, fromName)
        helper.setTo(email)
        helper.setSubject("[stockNews] 오늘의 관심종목 뉴스")
        helper.setText(buildHtml(articlesByTicker), true)
        mailSender.send(mimeMessage)
    }

    private fun buildHtml(articlesByTicker: Map<String, List<NewsArticle>>): String {
        val sections = StringBuilder()
        for ((ticker, articles) in articlesByTicker) {
            sections.append(
                """
                <tr>
                  <td style="padding:0 32px 24px 32px;">
                    <p style="margin:0 0 10px 0;color:#111827;font-size:15px;font-weight:700;">$ticker</p>
                """.trimIndent()
            )
            for (article in articles) {
                sections.append(
                    """
                    <p style="margin:0 0 8px 0;font-size:14px;">
                      <a href="${article.url}" style="color:#2563eb;text-decoration:none;">${article.title}</a>
                    </p>
                    """.trimIndent()
                )
            }
            sections.append("</td></tr>")
        }

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.06);">
                      <tr>
                        <td style="background-color:#111827;padding:28px 32px;">
                          <span style="color:#ffffff;font-size:20px;font-weight:700;letter-spacing:0.5px;">stockNews</span>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:36px 32px 20px 32px;">
                          <p style="margin:0;color:#111827;font-size:18px;font-weight:600;">오늘의 관심종목 뉴스</p>
                        </td>
                      </tr>
                      $sections
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }
}
