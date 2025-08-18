package com.sonsminpark.auratalkback.domain.auth.controller;

import com.sonsminpark.auratalkback.domain.auth.dto.request.TokenRefreshRequestDto;
import com.sonsminpark.auratalkback.domain.auth.dto.response.TokenResponseDto;
import com.sonsminpark.auratalkback.domain.auth.service.AuthService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshToken(
            @Valid @RequestBody TokenRefreshRequestDto request) {
        TokenResponseDto response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("토큰이 갱신되었습니다.", response));
    }
}