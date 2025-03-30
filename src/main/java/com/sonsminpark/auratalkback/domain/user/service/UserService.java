package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.request.LoginRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.request.ProfileSetupRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.request.SignUpRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.LoginResponseDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.SignUpResponseDto;

public interface UserService {
    LoginResponseDto login(LoginRequestDto loginRequestDto);

    void logout(String token);

    SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto);

    void setupProfile(Long userId, ProfileSetupRequestDto profileSetupRequestDto);
}