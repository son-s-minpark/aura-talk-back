package com.sonsminpark.auratalkback.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    private String tokenId;
    private Long userId;
    private String email;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
    private int rotationCount;
    private boolean isRevoked;

    public void revoke() {
        this.isRevoked = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public RefreshToken rotate() {
        return RefreshToken.builder()
                .tokenId(java.util.UUID.randomUUID().toString())
                .userId(this.userId)
                .email(this.email)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .rotationCount(this.rotationCount + 1)
                .isRevoked(false)
                .build();
    }
}