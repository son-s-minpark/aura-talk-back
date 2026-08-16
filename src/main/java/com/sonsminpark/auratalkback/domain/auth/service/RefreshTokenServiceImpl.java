package com.sonsminpark.auratalkback.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonsminpark.auratalkback.domain.auth.entity.RefreshToken;
import com.sonsminpark.auratalkback.domain.auth.exception.InvalidRefreshTokenException;
import com.sonsminpark.auratalkback.domain.auth.exception.RefreshTokenReuseDetectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REFRESH_TOKEN_PREFIX = "REFRESH_TOKEN:";
    private static final String USER_TOKEN_PREFIX = "USER_TOKENS:";
    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    @Override
    public RefreshToken createRefreshToken(Long userId, String email) {
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenId(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_VALIDITY_DAYS))
                .rotationCount(0)
                .isRevoked(false)
                .build();

        saveToken(refreshToken);
        addToUserTokens(userId, refreshToken.getTokenId());

        log.info("Refresh Token 생성 완료 - 사용자 ID: {}, 토큰 ID: {}", userId, refreshToken.getTokenId());
        return refreshToken;
    }

    @Override
    public Optional<RefreshToken> findByTokenId(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        String tokenData = redisTemplate.opsForValue().get(key);

        if (tokenData == null) {
            return Optional.empty();
        }

        try {
            RefreshToken token = objectMapper.readValue(tokenData, RefreshToken.class);
            return Optional.of(token);
        } catch (JsonProcessingException e) {
            log.error("Refresh Token 역직렬화 실패 - 토큰 ID: {}, 오류: {}", tokenId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void revokeToken(String tokenId) {
        String key = REFRESH_TOKEN_PREFIX + tokenId;
        Optional<RefreshToken> tokenOpt = findByTokenId(tokenId);

        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            token.revoke();
            saveToken(token);
            removeFromUserTokens(token.getUserId(), tokenId);
            log.info("Refresh Token 무효화 완료 - 토큰 ID: {}, 사용자 ID: {}", tokenId, token.getUserId());
        }
    }

    @Override
    public void revokeAllUserTokens(Long userId) {
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        Set<String> tokenIds = redisTemplate.opsForSet().members(userTokenKey);

        if (tokenIds != null) {
            for (String tokenId : tokenIds) {
                revokeToken(tokenId);
            }
        }

        redisTemplate.delete(userTokenKey);
        log.info("사용자 모든 Refresh Token 무효화 완료 - 사용자 ID: {}, 무효화된 토큰 수: {}",
                userId, tokenIds != null ? tokenIds.size() : 0);
    }

    @Override
    public boolean validateToken(String tokenId) {
        Optional<RefreshToken> tokenOpt = findByTokenId(tokenId);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        RefreshToken token = tokenOpt.get();

        if (token.isRevoked() || token.isExpired()) {
            return false;
        }

        return true;
    }

    @Override
    public RefreshToken rotateToken(String tokenId) {
        Optional<RefreshToken> oldTokenOpt = findByTokenId(tokenId);

        if (oldTokenOpt.isEmpty()) {
            throw InvalidRefreshTokenException.of("토큰을 찾을 수 없습니다.");
        }

        RefreshToken oldToken = oldTokenOpt.get();

        if (oldToken.isRevoked()) {
            // 토큰 재사용 감지
            log.warn("Refresh Token 재사용 감지 - 사용자 ID: {}, 토큰 ID: {}", oldToken.getUserId(), tokenId);
            revokeAllUserTokens(oldToken.getUserId());
            throw RefreshTokenReuseDetectedException.of();
        }

        if (oldToken.isExpired()) {
            throw InvalidRefreshTokenException.of("만료된 토큰입니다.");
        }

        revokeToken(tokenId);

        RefreshToken newToken = oldToken.rotate();
        saveToken(newToken);
        addToUserTokens(newToken.getUserId(), newToken.getTokenId());

        log.info("Refresh Token 회전 완료 - 사용자 ID: {}, 기존 토큰 ID: {}, 새 토큰 ID: {}",
                oldToken.getUserId(), tokenId, newToken.getTokenId());
        return newToken;
    }

    private void saveToken(RefreshToken token) {
        String key = REFRESH_TOKEN_PREFIX + token.getTokenId();
        try {
            String tokenData = objectMapper.writeValueAsString(token);
            long ttl = REFRESH_TOKEN_VALIDITY_DAYS * 24 * 60 * 60; // 초
            redisTemplate.opsForValue().set(key, tokenData, ttl, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("Refresh Token 직렬화 실패 - 토큰 ID: {}, 오류: {}", token.getTokenId(), e.getMessage());
            throw new RuntimeException("토큰 저장 중 오류가 발생했습니다.");
        }
    }

    private void addToUserTokens(Long userId, String tokenId) {
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        redisTemplate.opsForSet().add(userTokenKey, tokenId);
        redisTemplate.expire(userTokenKey, REFRESH_TOKEN_VALIDITY_DAYS, TimeUnit.DAYS);
    }

    private void removeFromUserTokens(Long userId, String tokenId) {
        String userTokenKey = USER_TOKEN_PREFIX + userId;
        redisTemplate.opsForSet().remove(userTokenKey, tokenId);
    }
}