package com.sonsminpark.auratalkback.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * 토큰을 블랙리스트에 추가합니다.
     *
     * @param token 블랙리스트에 추가할 JWT 토큰
     * @param expirationTime 토큰 만료까지 남은 시간(밀리초)
     */
    public void addToBlacklist(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", expirationTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 토큰이 블랙리스트에 있는지 확인합니다.
     *
     * @param token 확인할 JWT 토큰
     * @return 블랙리스트에 있으면 true, 아니면 false
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}