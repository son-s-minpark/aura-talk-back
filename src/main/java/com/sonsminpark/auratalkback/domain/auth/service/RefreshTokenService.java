package com.sonsminpark.auratalkback.domain.auth.service;

import com.sonsminpark.auratalkback.domain.auth.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(Long userId, String email);

    Optional<RefreshToken> findByTokenId(String tokenId);

    void revokeToken(String tokenId);

    void revokeAllUserTokens(Long userId);

    boolean validateToken(String tokenId);

    RefreshToken rotateToken(String tokenId);
}