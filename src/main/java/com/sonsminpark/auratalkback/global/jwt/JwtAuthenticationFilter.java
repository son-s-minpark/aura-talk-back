package com.sonsminpark.auratalkback.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;
import com.sonsminpark.auratalkback.global.security.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 특정 경로는 인증 체크를 건너뛰도록 설정
        if (shouldSkipAuthentication(request)) {
            log.debug("인증 체크 건너뛰기 - URI: {}, 메서드: {}", requestURI, method);
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        try {
            if (StringUtils.hasText(token)) {
                // 토큰이 블랙리스트에 있는지 확인
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.warn("블랙리스트된 토큰으로 접근 시도 - URI: {}", requestURI);
                    sendErrorResponse(response, ErrorCode.INVALID_AUTH_TOKEN, "로그아웃된 토큰입니다.");
                    return;
                }

                // 토큰 유효성 검증
                if (jwtTokenProvider.validateToken(token)) {
                    String tokenType = jwtTokenProvider.getTokenType(token);
                    if (!"ACCESS".equals(tokenType)) {
                        log.warn("잘못된 토큰 타입으로 API 접근 시도 - URI: {}, 토큰 타입: {}", requestURI, tokenType);
                        sendErrorResponse(response, ErrorCode.INVALID_AUTH_TOKEN,
                                "API 인증에는 Access Token을 사용해야 합니다.");
                        return;
                    }

                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("인증 성공 - 사용자: {}, URI: {}", authentication.getName(), requestURI);
                } else {
                    log.warn("유효하지 않은 JWT 토큰 - URI: {}", requestURI);
                    sendErrorResponse(response, ErrorCode.INVALID_AUTH_TOKEN, "유효하지 않은 토큰입니다.");
                    return;
                }
            } else if (!shouldBypassMissingTokenCheck(request)) {
                log.warn("인증 토큰 누락 - URI: {}", requestURI);
                sendErrorResponse(response, ErrorCode.UNAUTHORIZED, "인증 토큰이 필요합니다.");
                return;
            }
        } catch (Exception e) {
            log.error("인증 처리 중 오류 발생 - URI: {}, 오류: {}", requestURI, e.getMessage());
            sendErrorResponse(response, ErrorCode.INTERNAL_SERVER_ERROR, "인증 처리 중 오류가 발생했습니다.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> apiResponse = ApiResponse.error(errorCode, message);

        String jsonResponse = objectMapper.writeValueAsString(apiResponse);
        response.getWriter().write(jsonResponse);

        log.debug("에러 응답 전송 완료 - 상태 코드: {}, 메시지: {}", errorCode.getCode(), message);
    }

    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        return path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars") ||
                path.equals("/api/users/login") ||
                (path.equals("/api/users") && "POST".equalsIgnoreCase(method)) ||
                path.startsWith("/api/users/verify-email") ||
                path.startsWith("/api/users/resend-verification") ||
                path.equals("/api/health") ||
                path.equals("/api/auth/refresh") ||
                path.equals("/api/interests") ||
                path.startsWith("/api/interests/category/") ||
                path.startsWith("/ws/") ||
                path.equals("/ws");
    }

    private boolean shouldBypassMissingTokenCheck(HttpServletRequest request) {
        // 인증이 필요하지만 토큰이 없어도 되는 경우
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}