package com.sonsminpark.auratalkback.domain.auth.service;

import com.sonsminpark.auratalkback.domain.auth.dto.response.TokenResponseDto;
import com.sonsminpark.auratalkback.domain.auth.entity.RefreshToken;
import com.sonsminpark.auratalkback.domain.auth.exception.InvalidRefreshTokenException;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public TokenResponseDto refreshToken(String refreshTokenJwt) {
        log.debug("토큰 갱신 요청 시작 - JWT 토큰 길이: {}", refreshTokenJwt.length());

        if (!jwtTokenProvider.validateToken(refreshTokenJwt)) {
            log.warn("유효하지 않은 JWT 토큰으로 갱신 시도");
            throw InvalidRefreshTokenException.of("유효하지 않은 토큰 형식입니다.");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshTokenJwt);
        if (!"REFRESH".equals(tokenType)) {
            log.warn("잘못된 토큰 타입으로 갱신 시도 - 토큰 타입: {}", tokenType);
            throw InvalidRefreshTokenException.of("Refresh Token이 아닙니다.");
        }

        String tokenId = jwtTokenProvider.getTokenIdFromRefreshToken(refreshTokenJwt);
        log.debug("토큰 ID 추출 완료 - 토큰 ID: {}", tokenId);

        RefreshToken newRefreshToken = refreshTokenService.rotateToken(tokenId);

        String newAccessToken = jwtTokenProvider.createAccessToken(
                newRefreshToken.getEmail(), newRefreshToken.getUserId());
        String newRefreshTokenJwt = jwtTokenProvider.createRefreshTokenJwt(
                newRefreshToken.getTokenId(), newRefreshToken.getEmail(), newRefreshToken.getUserId());

        log.info("토큰 갱신 성공 - 사용자 ID: {}, 이메일: {}, 새 토큰 ID: {}",
                newRefreshToken.getUserId(), newRefreshToken.getEmail(), newRefreshToken.getTokenId());

        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenJwt)
                .build();
    }

    @Override
    public TokenResponseDto createTokenPair(Long userId, String email) {
        log.debug("토큰 쌍 생성 요청 - 사용자 ID: {}, 이메일: {}", userId, email);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userId, email);

        String accessToken = jwtTokenProvider.createAccessToken(email, userId);
        String refreshTokenJwt = jwtTokenProvider.createRefreshTokenJwt(
                refreshToken.getTokenId(), email, userId);

        log.info("토큰 쌍 생성 완료 - 사용자 ID: {}, 이메일: {}, 토큰 ID: {}",
                userId, email, refreshToken.getTokenId());

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenJwt)
                .build();
    }

    @Override
    public void revokeAllUserTokens(Long userId) {
        log.debug("사용자 모든 토큰 무효화 요청 - 사용자 ID: {}", userId);

        refreshTokenService.revokeAllUserTokens(userId);

        log.info("사용자 모든 토큰 무효화 완료 - 사용자 ID: {}", userId);
    }
}