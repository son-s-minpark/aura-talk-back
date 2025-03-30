package com.sonsminpark.auratalkback.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
        // TODO: 이메일 발송 로직 구현하기
        // 로컬 환경에서 콘솔에 토큰을 출력하여 확인 가능
        log.info("Verification email would be sent to: {}", email);
        log.info("Verification token: {}", token);
        log.info("Please use this token to verify your email within 24 hours.");
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
}