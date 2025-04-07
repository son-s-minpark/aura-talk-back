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

        // 특정 경로는 인증 체크를 건너뛰도록 설정
        if (shouldSkipAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        try {
            if (StringUtils.hasText(token)) {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.debug("Blacklisted JWT token found, uri: {}", request.getRequestURI());
                    sendErrorResponse(response, ErrorCode.INVALID_AUTH_TOKEN, "로그아웃된 토큰입니다.");
                    return;
                } else if (jwtTokenProvider.validateToken(token)) {
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Set Authentication to security context for '{}', uri: {}",
                            authentication.getName(), request.getRequestURI());
                } else {
                    log.debug("Invalid JWT token, uri: {}", request.getRequestURI());
                    sendErrorResponse(response, ErrorCode.INVALID_AUTH_TOKEN, "유효하지 않은 토큰입니다.");
                    return;
                }
            } else if (!shouldBypassMissingTokenCheck(request)) {
                log.debug("No JWT token found, uri: {}", request.getRequestURI());
                sendErrorResponse(response, ErrorCode.UNAUTHORIZED, "인증 토큰이 필요합니다.");
                return;
            }
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
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
                path.startsWith("/api/users/resend-verification");
    }

    private boolean shouldBypassMissingTokenCheck(HttpServletRequest request) {
        // 인증이 필요하지만 토큰이 없어도 되는 경우
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}