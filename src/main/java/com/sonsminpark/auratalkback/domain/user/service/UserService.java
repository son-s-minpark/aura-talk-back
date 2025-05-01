package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.request.*;
import com.sonsminpark.auratalkback.domain.user.dto.response.LoginResponseDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.SignUpResponseDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.UserResponseDto;

public interface UserService {
    // 로그인
    LoginResponseDto login(LoginRequestDto loginRequestDto);

    // 로그아웃
    void logout(String token);

    // 회원가입
    SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto);

    // 회원탈퇴
    void deleteUser(String token, UserDeleteRequestDto userDeleteRequestDto);

    // 프로필 설정
    void setupProfile(String token, ProfileSetupRequestDto profileSetupRequestDto);

    // 이메일 인증
    boolean verifyEmail(EmailVerificationRequestDto emailVerificationRequestDto);

    // 이메일 인증 메일 재전송
    void resendVerificationEmail(String email);

    // 프로필 조회
    UserResponseDto getUserProfile(String token);

    // 랜덤 채팅 설정 업데이트
    void updateChatSettings(String token, boolean randomChatEnabled);
}