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
        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage)
        helper.setFrom(fromAddress, fromName)
        helper.setTo(email)
        helper.setSubject("[stockNews] 이메일 인증코드")
        helper.setText("인증코드: $code\n${expiryMinutes}분 이내에 입력해주세요.")
        mailSender.send(mimeMessage)
    }
}
