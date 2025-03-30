package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.request.EmailVerificationRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.request.LoginRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.request.ProfileSetupRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.request.SignUpRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.LoginResponseDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.SignUpResponseDto;

public interface UserService {
    // 로그인
    LoginResponseDto login(LoginRequestDto loginRequestDto);

    // 로그아웃
    void logout(String token);

    // 회원가입
    SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto);

    // 프로필 설정
    void setupProfile(Long userId, ProfileSetupRequestDto profileSetupRequestDto);

    // 이메일 인증
    boolean verifyEmail(EmailVerificationRequestDto emailVerificationRequestDto);

    // 이메일 인증 메일 재전송
    void resendVerificationEmail(String email);
}