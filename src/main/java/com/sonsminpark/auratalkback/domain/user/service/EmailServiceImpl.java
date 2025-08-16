package com.sonsminpark.auratalkback.domain.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final long VERIFICATION_TOKEN_VALIDITY = 10 * 60 * 1000; // 10분
    private static final String EMAIL_VERIFICATION_PREFIX = "EMAIL_VERIFICATION:";
    private final JavaMailSender mailSender;
    private final Random random = new Random();

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(String email, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("AuraTalk 회원가입 이메일 인증");

            String htmlContent = createVerificationEmailTemplate(token);
            helper.setText(htmlContent, true); // HTML 형식 지원

            mailSender.send(message);

            log.info("Verification email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send verification email", e);
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    @Override
    public String generateVerificationToken(String email) {
        // 6자리 랜덤 숫자 생성 (100000 ~ 999999)
        String token = String.format("%06d", random.nextInt(900000) + 100000);
        String key = EMAIL_VERIFICATION_PREFIX + email;

        // Redis에 이메일과 인증번호 매핑 저장 (10분)
        redisTemplate.opsForValue().set(key, token, VERIFICATION_TOKEN_VALIDITY, TimeUnit.MILLISECONDS);

        log.info("Generated 6-digit verification code for email: {}", email);
        return token;
    }

    @Override
    public boolean validateVerificationToken(String email, String token) {
        String key = EMAIL_VERIFICATION_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken != null && storedToken.equals(token)) {
            // 인증 성공 시 토큰 삭제
            redisTemplate.delete(key);
            log.info("Email verification successful for: {}", email);
            return true;
        }

        log.warn("Email verification failed for: {} with token: {}", email, token);
        return false;
    }

    // 이메일 인증용 HTML 템플릿 생성
    private String createVerificationEmailTemplate(String verificationCode) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>본인 확인을 위한 메일 인증</title>\n" +
                "    <style>\n" +
                "        body { \n" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; \n" +
                "            line-height: 1.6; \n" +
                "            margin: 0; \n" +
                "            padding: 0; \n" +
                "            background-color: #f8f9fa; \n" +
                "        }\n" +
                "        .container { \n" +
                "            max-width: 480px; \n" +
                "            margin: 40px auto; \n" +
                "            background-color: white; \n" +
                "            border-radius: 16px; \n" +
                "            overflow: hidden; \n" +
                "            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08); \n" +
                "        }\n" +
                "        .header { \n" +
                "            background-color: white; \n" +
                "            padding: 40px 30px 20px; \n" +
                "            text-align: center; \n" +
                "            border-bottom: 1px solid #f0f0f0; \n" +
                "        }\n" +
                "        .logo-container { \n" +
                "            display: flex; \n" +
                "            align-items: center; \n" +
                "            justify-content: center; \n" +
                "            margin-bottom: 20px; \n" +
                "        }\n" +
                "        .logo-text { \n" +
                "            font-family: 'Helvetica Neue', 'Arial', sans-serif; \n" +
                "            font-size: 28px; \n" +
                "            font-weight: 700; \n" +
                "            color: #6EABFF; \n" +
                "            letter-spacing: 2px; \n" +
                "            margin: 0; \n" +
                "        }\n" +
                "        .title { \n" +
                "            font-size: 20px; \n" +
                "            font-weight: 600; \n" +
                "            color: #333; \n" +
                "            margin: 0; \n" +
                "        }\n" +
                "        .highlight { \n" +
                "            color: #6EABFF; \n" +
                "        }\n" +
                "        .content { \n" +
                "            padding: 30px; \n" +
                "            text-align: center; \n" +
                "        }\n" +
                "        .description { \n" +
                "            font-size: 15px; \n" +
                "            color: #666; \n" +
                "            margin-bottom: 30px; \n" +
                "            line-height: 1.5; \n" +
                "        }\n" +
                "        .verification-section { \n" +
                "            background-color: #6EABFF; \n" +
                "            border-radius: 12px; \n" +
                "            padding: 24px; \n" +
                "            margin: 20px 0; \n" +
                "        }\n" +
                "        .verification-label { \n" +
                "            font-size: 14px; \n" +
                "            color: white; \n" +
                "            margin-bottom: 12px; \n" +
                "            font-weight: 500; \n" +
                "        }\n" +
                "        .verification-code { \n" +
                "            font-size: 32px; \n" +
                "            font-weight: 700; \n" +
                "            color: white; \n" +
                "            letter-spacing: 6px; \n" +
                "            margin: 0; \n" +
                "            font-family: 'Courier New', monospace; \n" +
                "        }\n" +
                "        .notice { \n" +
                "            font-size: 13px; \n" +
                "            color: #888; \n" +
                "            margin-top: 20px; \n" +
                "            line-height: 1.4; \n" +
                "        }\n" +
                "        .footer { \n" +
                "            background-color: #f8f9fa; \n" +
                "            padding: 20px; \n" +
                "            text-align: center; \n" +
                "        }\n" +
                "        .footer-text { \n" +
                "            font-size: 12px; \n" +
                "            color: #999; \n" +
                "            margin: 4px 0; \n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <div class=\"logo-container\">\n" +
                "                <img src=\"https://github.com/user-attachments/assets/d9f0d150-3373-4ad3-bfb2-9b1a9745801a\" alt=\"AURATALK\" class=\"logo-image\">\n" +
                "            </div>\n" +
                "            <h1 class=\"title\">본인 확인을 위한 <span class=\"highlight\">메일 인증</span></h1>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"content\">\n" +
                "            <p class=\"description\">\n" +
                "                개인정보 보호를 위한 메일 인증을 진행하고 있어요.<br>\n" +
                "                하단의 인증번호를 확인해주세요.\n" +
                "            </p>\n" +
                "            \n" +
                "            <div class=\"verification-section\">\n" +
                "                <div class=\"verification-label\">인증번호</div>\n" +
                "                <div class=\"verification-code\">" + verificationCode + "</div>\n" +
                "            </div>\n" +
                "            \n" +
                "            <p class=\"notice\">\n" +
                "                인증번호 입력 시간 내에 입력해주세요.<br>\n" +
                "                (유효시간: 10분)\n" +
                "            </p>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"footer\">\n" +
                "            <p class=\"footer-text\">본 메일은 발신 전용이며 회신되지 않습니다.</p>\n" +
                "            <p class=\"footer-text\">&copy; 2025 AuraTalk. All rights reserved.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}