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
        val cards = StringBuilder()
        for ((ticker, articles) in articlesByTicker) {
            for (article in articles) {
                cards.append(
                    """
                    <tr>
                      <td style="padding:0 32px 16px 32px;">
                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f9fafb;border:1px solid #eef0f3;border-left:4px solid #2563eb;border-radius:10px;">
                          <tr>
                            <td style="padding:18px 20px;">
                              <span style="display:inline-block;background-color:#111827;color:#ffffff;font-size:11px;font-weight:700;letter-spacing:0.4px;padding:4px 10px;border-radius:999px;">$ticker</span>
                              <p style="margin:12px 0 14px 0;color:#111827;font-size:15px;font-weight:600;line-height:1.5;">${article.title}</p>
                              <a href="${article.url}" style="display:inline-block;color:#2563eb;text-decoration:none;font-size:13px;font-weight:600;">기사 보러가기 →</a>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    """.trimIndent()
                )
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <body style="margin:0;padding:0;background-color:#f4f5f7;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f5f7;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="520" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:14px;overflow:hidden;box-shadow:0 4px 16px rgba(17,24,39,0.08);">
                      <tr>
                        <td style="background:linear-gradient(135deg,#111827,#1f2937);padding:32px;">
                          <span style="color:#ffffff;font-size:20px;font-weight:700;letter-spacing:0.5px;">📈 stockNews</span>
                          <p style="margin:8px 0 0 0;color:#9ca3af;font-size:13px;">관심 종목 데일리 뉴스 다이제스트</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:28px 32px 4px 32px;">
                          <p style="margin:0;color:#111827;font-size:18px;font-weight:700;">오늘의 관심종목 뉴스</p>
                          <p style="margin:6px 0 20px 0;color:#6b7280;font-size:13px;">구독 중인 ${articlesByTicker.size}개 종목의 최신 소식이에요.</p>
                        </td>
                      </tr>
                      $cards
                      <tr>
                        <td style="padding:12px 32px 28px 32px;border-top:1px solid #f0f1f3;">
                          <p style="margin:16px 0 0 0;color:#9ca3af;font-size:11px;">이 메일은 stockNews에 등록하신 관심 종목을 기준으로 발송되었습니다.</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }
}
