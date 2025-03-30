package com.sonsminpark.auratalkback.domain.user.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.swing.text.html.HTML;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final long VERIFICATION_TOKEN_VALIDITY = 24 * 60 * 60 * 1000; // 24시간
    private static final String EMAIL_VERIFICATION_PREFIX = "EMAIL_VERIFICATION:";

    @Override
    public void sendVerificationEmail(String email, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("AuraTalk 회원가입 이메일 인증");

            String htmlContent = createVerificationEmailTemplate(email, token);
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
        String token = UUID.randomUUID().toString();
        String key = EMAIL_VERIFICATION_PREFIX + email;

        // Redis에 이메일과 토큰 매핑 저장
        redisTemplate.opsForValue().set(key, token, VERIFICATION_TOKEN_VALIDITY, TimeUnit.MILLISECONDS);

        return token;
    }

    @Override
    public boolean validateVerificationToken(String email, String token) {
        String key = EMAIL_VERIFICATION_PREFIX + email;
        String storedToken = redisTemplate.opsForValue().get(key);

        if (storedToken != null && storedToken.equals(token)) {
            // 인증 성공 시 토큰 삭제
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    // 이메일 인증용 HTML 템플릿 생성
    private String createVerificationEmailTemplate(String email, String token) {
        // TODO: 프론트엔드에서 처리할 인증 URL 수정하기
        String verificationUrl = "http://localhost:3000/verify-email?email=" + email + "&token=" + token;

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>이메일 인증</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
                "        .header { background-color: #5E35B1; color: white; padding: 10px; text-align: center; }\n" +
                "        .content { padding: 20px; background-color: #f8f9fa; }\n" +
                "        .button { display: inline-block; padding: 10px 20px; background-color: #5E35B1; color: white; text-decoration: none; border-radius: 5px; }\n" +
                "        .footer { font-size: 12px; color: #6c757d; margin-top: 20px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <div class=\"header\">\n" +
                "            <h1>AuraTalk 이메일 인증</h1>\n" +
                "        </div>\n" +
                "        <div class=\"content\">\n" +
                "            <p>안녕하세요!</p>\n" +
                "            <p>AuraTalk 회원가입을 완료하기 위해 아래 버튼을 클릭하여 이메일 인증을 진행해주세요.</p>\n" +
                "            <p><a href=\"" + verificationUrl + "\" class=\"button\">이메일 인증하기</a></p>\n" +
                "            <p>또는 다음 인증 코드를 입력해주세요: <strong>" + token + "</strong></p>\n" +
                "            <p>이 인증 링크는 24시간 동안 유효합니다.</p>\n" +
                "        </div>\n" +
                "        <div class=\"footer\">\n" +
                "            <p>본 이메일은 발신 전용이며 회신되지 않습니다.</p>\n" +
                "            <p>&copy; 2025 AuraTalk. All rights reserved.</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }
}