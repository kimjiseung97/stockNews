package org.kjs.stocknews.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

@Component
class VerificationMailSender(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username}") private val fromAddress: String,
    @Value("\${mail.from-name}") private val fromName: String,
) {
    fun sendVerificationCode(email: String, code: String, expiryMinutes: Long) {
        send(
            email = email,
            subject = "[stockNews] 이메일 인증코드",
            description = "아래 인증코드를 회원가입 화면에 입력해주세요.",
            code = code,
            expiryMinutes = expiryMinutes,
        )
    }

    fun sendFindEmailCode(email: String, code: String, expiryMinutes: Long) {
        send(
            email = email,
            subject = "[stockNews] 이메일 찾기 인증코드",
            description = "아래 인증코드를 이메일 찾기 화면에 입력해주세요.",
            code = code,
            expiryMinutes = expiryMinutes,
        )
    }

    private fun send(email: String, subject: String, description: String, code: String, expiryMinutes: Long) {
        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage)
        helper.setFrom(fromAddress, fromName)
        helper.setTo(email)
        helper.setSubject(subject)
        helper.setText(buildHtml(description, code, expiryMinutes), true)
        mailSender.send(mimeMessage)
    }

    private fun buildHtml(description: String, code: String, expiryMinutes: Long): String = """
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
                    <td style="padding:36px 32px 24px 32px;">
                      <p style="margin:0 0 8px 0;color:#111827;font-size:18px;font-weight:600;">이메일 인증코드</p>
                      <p style="margin:0;color:#6b7280;font-size:14px;line-height:1.6;">
                        $description<br/>
                        인증코드는 <strong>${expiryMinutes}분</strong> 동안 유효합니다.
                      </p>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:0 32px 36px 32px;">
                      <div style="background-color:#f4f5f7;border-radius:8px;padding:20px;text-align:center;">
                        <span style="font-size:32px;font-weight:700;letter-spacing:8px;color:#111827;">$code</span>
                      </div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:0 32px 32px 32px;border-top:1px solid #e5e7eb;">
                      <p style="margin:20px 0 0 0;color:#9ca3af;font-size:12px;line-height:1.6;">
                        본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.
                      </p>
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
