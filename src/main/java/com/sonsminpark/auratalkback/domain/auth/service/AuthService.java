package com.sonsminpark.auratalkback.domain.auth.service;

import com.sonsminpark.auratalkback.domain.auth.dto.response.TokenResponseDto;

public interface AuthService {
    TokenResponseDto refreshToken(String refreshTokenJwt);

    TokenResponseDto createTokenPair(Long userId, String email);

    void revokeAllUserTokens(Long userId);
}