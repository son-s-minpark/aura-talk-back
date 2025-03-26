package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.request.LoginRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.request.ProfileSetupRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.request.SignUpRequestDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.LoginResponseDto;
import com.sonsminpark.auratalkback.domain.user.dto.response.UserResponseDto;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import com.sonsminpark.auratalkback.domain.user.exception.DuplicateUserException;
import com.sonsminpark.auratalkback.domain.user.exception.InvalidUserCredentialsException;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {

        User user = userRepository.findByEmailAndIsDeletedFalse(loginRequestDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException(loginRequestDto.getEmail(), "존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new InvalidUserCredentialsException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 로그인 시 ONLINE으로 변경
        user.updateStatus(UserStatus.ONLINE);

        String token = jwtTokenProvider.createToken(user.getEmail());

        UserResponseDto userResponseDto = UserResponseDto.from(user);

        return LoginResponseDto.builder()
                .token(token)
                .user(userResponseDto)
                .build();
    }

    @Override
    @Transactional
    public void logout(String token) {

        String email = jwtTokenProvider.getEmailFromToken(token);


        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new UserNotFoundException(email, "존재하지 않는 사용자입니다."));

        // 로그아웃 시 OFFLINE으로 변경
        user.updateStatus(UserStatus.OFFLINE);

        // 토큰 블랙리스트에 추가
        long validityInMilliseconds = jwtTokenProvider.getTokenValidityInMilliseconds();
        redisTemplate.opsForValue().set("BLACKLIST:" + token, "logout", validityInMilliseconds, TimeUnit.MILLISECONDS);
    }

    @Override
    @Transactional
    public void signUp(SignUpRequestDto signUpRequestDto) {
        // 이메일 중복 확인
        if (userRepository.existsByEmailAndIsDeletedFalse(signUpRequestDto.getEmail())) {
            throw DuplicateUserException.ofEmail(signUpRequestDto.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());

        User user = User.builder()
                .email(signUpRequestDto.getEmail())
                .password(encodedPassword)
                .username("사용자명")
                .nickname("닉네임")
                .interests(new ArrayList<>())
                .status(UserStatus.OFFLINE)
                .isDeleted(false)
                .build();

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void setupProfile(Long userId, ProfileSetupRequestDto profileSetupRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 사용자명 중복 확인
        if (userRepository.existsByUsernameAndIsDeletedFalse(profileSetupRequestDto.getUsername()) &&
                !user.getUsername().equals(profileSetupRequestDto.getUsername())) {
            throw DuplicateUserException.ofUsername(profileSetupRequestDto.getUsername());
        }

        // 닉네임 중복 확인
        if (userRepository.existsByNicknameAndIsDeletedFalse(profileSetupRequestDto.getNickname()) &&
                !user.getNickname().equals(profileSetupRequestDto.getNickname())) {
            throw DuplicateUserException.ofNickname(profileSetupRequestDto.getNickname());
        }

        user.updateProfile(
                profileSetupRequestDto.getUsername(),
                profileSetupRequestDto.getNickname(),
                profileSetupRequestDto.getInterests()
        );
    }
}