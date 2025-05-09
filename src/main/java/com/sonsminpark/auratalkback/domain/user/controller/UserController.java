package com.sonsminpark.auratalkback.domain.user.controller;

import com.sonsminpark.auratalkback.domain.user.dto.request.*;
import com.sonsminpark.auratalkback.domain.user.dto.response.LoginResponseDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.SignUpResponseDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.UserResponseDto;
import com.sonsminpark.auratalkback.domain.user.service.UserService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @Operation(
            summary = "로그아웃",
            description = "현재 로그인된 사용자를 로그아웃 처리합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        userService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("로그아웃에 성공했습니다."));
    }

    @PostMapping
    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 회원가입하고 토큰을 발급받습니다.")
    public ResponseEntity<ApiResponse<SignUpResponseDto>> signUp(@Valid @RequestBody SignUpRequestDto signUpRequestDto) {
        SignUpResponseDto signUpResponseDto = userService.signUp(signUpRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 성공적으로 완료되었습니다.", signUpResponseDto));
    }

    @DeleteMapping("/{userId}")
    @Operation(
            summary = "회원탈퇴",
            description = "회원 탈퇴 처리를 합니다. 30일간 정보를 보관 후 삭제됩니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserDeleteRequestDto userDeleteRequestDto) {
        userService.deleteUser(userId, userDeleteRequestDto);
        return ResponseEntity.ok(ApiResponse.success("회원탈퇴가 성공적으로 처리되었습니다. 30일 이내 재가입 시 계정을 복구할 수 있습니다."));
    }

    @PutMapping("/{userId}/profile")
    @Operation(
            summary = "프로필 설정",
            description = "회원가입 후 사용자 프로필 정보를 설정합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<Void>> setupProfile(
            @PathVariable Long userId,
            @Valid @RequestBody ProfileSetupRequestDto profileSetupRequestDto) {
        userService.setupProfile(userId, profileSetupRequestDto);
        return ResponseEntity.ok(ApiResponse.success("프로필 설정이 완료되었습니다."));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "이메일 인증", description = "회원가입 후 이메일 인증을 진행합니다.")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody EmailVerificationRequestDto emailVerificationRequestDto) {
        boolean isVerified = userService.verifyEmail(emailVerificationRequestDto);

        if (isVerified) {
            return ResponseEntity.ok(ApiResponse.success("이메일 인증이 성공적으로 완료되었습니다."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 인증 토큰입니다."));
        }
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "인증 이메일 재전송", description = "이메일 인증 메일을 재전송합니다.")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@Valid @RequestBody EmailResendRequestDto requestDto) {
        userService.resendVerificationEmail(requestDto.getEmail());
        return ResponseEntity.ok(ApiResponse.success("인증 이메일이 재전송되었습니다."));
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "프로필 조회",
            description = "사용자 프로필 정보를 조회합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserProfile(@PathVariable Long userId) {
        UserResponseDto userResponseDto = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("프로필 조회에 성공했습니다.", userResponseDto));
    }

    @PutMapping("/{userId}/chat-settings")
    @Operation(
            summary = "랜덤 채팅 설정",
            description = "랜덤 채팅 매칭 활성화/비활성화 설정을 변경합니다.",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    public ResponseEntity<ApiResponse<Void>> updateChatSettings(
            @PathVariable Long userId,
            @Valid @RequestBody ChatSettingsRequestDto chatSettingsRequestDto) {
        userService.updateChatSettings(userId, chatSettingsRequestDto.isRandomChatEnabled());
        return ResponseEntity.ok(ApiResponse.success("랜덤 채팅 설정이 변경되었습니다."));
    }
}