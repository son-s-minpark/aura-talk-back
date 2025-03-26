package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.request.LoginRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.request.UserDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.LoginResponseDto;

public interface UserService {
    LoginResponseDto login(LoginRequestDto loginRequestDto);
    void logout(String token);
    Long signUp(UserDto.SignUpRequest signUpRequest);
}