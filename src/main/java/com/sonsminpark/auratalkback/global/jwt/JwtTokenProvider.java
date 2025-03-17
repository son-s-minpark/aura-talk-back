package com.sonsminpark.auratalkback.global.jwt;

import com.sonsminpark.auratalkback.global.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:defaultSecretKey}")
    private String secretKey;

    @Value("${jwt.token-validity-in-milliseconds:86400000}") // 24시간
    private long tokenValidityInMilliseconds;

    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private SecretKey key;

    public JwtTokenProvider(UserDetailsService userDetailsService, TokenBlacklistService tokenBlacklistService) {
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getEncoder().encode(secretKey.getBytes(StandardCharsets.UTF_8));
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            // 토큰이 블랙리스트에 있는지 확인
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.error("Token is blacklisted: {}", token);
                return false;
            }

            // 토큰 유효성 검증
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(getEmailFromToken(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * 토큰의 남은 유효 시간을 계산합니다.
     *
     * @param token JWT 토큰
     * @return 남은 유효 시간(밀리초)
     */
    public long getTokenExpirationTime(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date expiration = claims.getExpiration();
        Date now = new Date();
        return Math.max(0, expiration.getTime() - now.getTime());
    }

    /**
     * 토큰을 블랙리스트에 추가합니다.
     *
     * @param token 블랙리스트에 추가할 JWT 토큰
     */
    public void blacklistToken(String token) {
        long remainingTime = getTokenExpirationTime(token);
        tokenBlacklistService.addToBlacklist(token, remainingTime);
    }
}