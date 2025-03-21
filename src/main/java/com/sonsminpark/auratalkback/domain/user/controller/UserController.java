package com.sonsminpark.auratalkback.domain.user.controller;

import com.sonsminpark.auratalkback.domain.user.dto.request.LoginRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.LoginResponseDto;
import com.sonsminpark.auratalkback.domain.user.service.UserService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호를 통해 로그인합니다.")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        LoginResponseDto loginResponseDto = userService.login(loginRequestDto);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", loginResponseDto));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자를 로그아웃 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        userService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("로그아웃에 성공했습니다."));
    }
}