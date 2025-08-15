package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.friend.entity.FriendStatus;
import com.sonsminpark.auratalkback.domain.friend.service.FriendService;
import com.sonsminpark.auratalkback.domain.user.dto.request.*;
import com.sonsminpark.auratalkback.domain.user.dto.response.*;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import com.sonsminpark.auratalkback.domain.user.exception.DuplicateUserException;
import com.sonsminpark.auratalkback.domain.user.exception.InvalidUserCredentialsException;
import com.sonsminpark.auratalkback.domain.user.exception.InvalidUserInputException;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import com.sonsminpark.auratalkback.global.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;
//import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;
    private final UserProfileImageService userProfileImageService;
    private final FriendService friendService;
    private final UserStatusNotificationService userStatusNotificationService;

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmailAndIsDeletedFalse(loginRequestDto.getEmail())
                .orElseThrow(() -> UserNotFoundException.of(loginRequestDto.getEmail(), "존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw InvalidUserCredentialsException.of("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 로그인 시 ONLINE으로 변경
        user.updateStatus(UserStatus.ONLINE);

        // userId를 포함하여 토큰 생성
        String token = jwtTokenProvider.createToken(user.getEmail(), user.getId());

        MyProfileResponseDto userResponseDto = MyProfileResponseDto.from(user);

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
                .orElseThrow(() -> UserNotFoundException.of(email, "존재하지 않는 사용자입니다."));

        // 로그아웃 시 OFFLINE으로 변경
        user.updateStatus(UserStatus.OFFLINE);

        // 토큰 블랙리스트에 추가
        tokenBlacklistService.addToBlacklist(token, jwtTokenProvider.getTokenValidityInMilliseconds());
    }

    @Override
    @Transactional
    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        // 이메일 중복 확인
        if (userRepository.existsByEmailAndIsDeletedFalse(signUpRequestDto.getEmail())) {
            throw DuplicateUserException.ofEmail(signUpRequestDto.getEmail());
        }

        // 30일 이내 탈퇴한 사용자가 있는지 확인
        Optional<User> deletedUser = userRepository.findByEmailAndIsDeletedTrue(signUpRequestDto.getEmail());
        if (deletedUser.isPresent()) {
            User user = deletedUser.get();

            // 탈퇴한 계정 복구 (30일 이내)
            String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());

            user = User.builder()
                    .id(user.getId())
                    .email(signUpRequestDto.getEmail())
                    .password(encodedPassword)
                    .username("사용자명")
                    .nickname("닉네임")
                    .status(UserStatus.ONLINE)
                    .isDeleted(false)
                    .deletedAt(null)
                    .emailVerified(true) // TODO: 이메일 인증 활성화 시 해당 줄 제거하기
                    .userInterests(new ArrayList<>())
                    .randomChatEnabled(false)
                    .createdAt(user.getCreatedAt())
                    .build();

            User savedUser = userRepository.save(user);

            ProfileImageResponseDto profileImageDto = userProfileImageService.createDefaultProfileImage(savedUser.getId());

            String token = jwtTokenProvider.createToken(savedUser.getEmail(), savedUser.getId());

            MyProfileResponseDto userResponseDto = MyProfileResponseDto.builder()
                    .id(savedUser.getId())
                    .email(savedUser.getEmail())
                    .username(savedUser.getUsername())
                    .nickname(savedUser.getNickname())
                    .description(savedUser.getDescription())
                    .interests(savedUser.getInterests())
                    .status(savedUser.getStatus())
                    .randomChatEnabled(savedUser.isRandomChatEnabled())
                    .createdAt(savedUser.getCreatedAt())
                    .profileImage(profileImageDto)
                    .build();

            return SignUpResponseDto.builder()
                    .userId(savedUser.getId())
                    .token(token)
                    .user(userResponseDto)
                    .build();
        }

        // 30일 후 또는 새로운 사용자 - 새 계정 생성
        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());

        User user = User.builder()
                .email(signUpRequestDto.getEmail())
                .password(encodedPassword)
                .username("임시 사용자명")
                .nickname("임시 닉네임")
                .status(UserStatus.ONLINE)
                .isDeleted(false)
                .emailVerified(true) // TODO: 이메일 인증 활성화 시 해당 줄 제거하기
                .build();

        User savedUser = userRepository.save(user);

        ProfileImageResponseDto profileImageDto = userProfileImageService.createDefaultProfileImage(savedUser.getId());

        // TODO: 이메일 인증 활성화 시 아래 주석 제거하기
//        String verificationToken = emailService.generateVerificationToken(savedUser.getEmail());
//        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        // 토큰에 userId 추가
        String token = jwtTokenProvider.createToken(savedUser.getEmail(), savedUser.getId());

        MyProfileResponseDto userResponseDto = MyProfileResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .nickname(savedUser.getNickname())
                .description(savedUser.getDescription())
                .interests(savedUser.getInterests())
                .status(savedUser.getStatus())
                .randomChatEnabled(savedUser.isRandomChatEnabled())
                .createdAt(savedUser.getCreatedAt())
                .profileImage(profileImageDto)
                .build();

        return SignUpResponseDto.builder()
                .userId(savedUser.getId())
                .token(token)
                .user(userResponseDto)
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(String token, UserDeleteRequestDto userDeleteRequestDto) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        // 이미 탈퇴한 회원인지 확인
        if (user.isDeleted()) {
            throw InvalidUserInputException.of("이미 탈퇴한 회원입니다.");
        }

        if (!passwordEncoder.matches(userDeleteRequestDto.getPassword(), user.getPassword())) {
            throw InvalidUserCredentialsException.of("비밀번호가 일치하지 않습니다.");
        }

        user.delete();

        // 인증 토큰 관련 처리
        tokenBlacklistService.addToBlacklist(token, jwtTokenProvider.getTokenValidityInMilliseconds());

        // 사용자 삭제 예약 (30일 후)
        scheduleUserDeletion(userId);
    }

    // 사용자 정보를 30일 후 완전 삭제합니다.
    private void scheduleUserDeletion(Long userId) {
        String key = "USER_DELETION:" + userId;
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
        tokenBlacklistService.addToBlacklist(key, thirtyDaysInMillis); // 30일 후 만료
    }

    @Override
    @Transactional
    public void setupProfile(String token, ProfileSetupRequestDto profileSetupRequestDto) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

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
                profileSetupRequestDto.getInterests(),
                profileSetupRequestDto.getDescription()
        );
    }

    @Override
    @Transactional
    public boolean verifyEmail(EmailVerificationRequestDto emailVerificationRequestDto) {
        // TODO: 항상 성공을 반환하므로 이메일 인증 활성화 시 아래 주석 제거하기
        /*if (!emailService.validateVerificationToken(
                emailVerificationRequestDto.getEmail(),
                emailVerificationRequestDto.getToken())) {
            return false;
        }

        User user = userRepository.findByEmailAndIsDeletedFalse(emailVerificationRequestDto.getEmail())
                .orElseThrow(() -> UserNotFoundException.of(emailVerificationRequestDto.getEmail(), "존재하지 않는 사용자입니다."));

        user.verifyEmail();*/

        return true;
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        // TODO: 아무 값도 반환하지 않으므로 이메일 인증 활성화 시 아래 주석 제거하기
        /*User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> UserNotFoundException.of(email, "존재하지 않는 사용자입니다."));

        // 이미 인증된 경우
        if (user.isEmailVerified()) {
            throw InvalidUserInputException.of("이미 인증된 이메일입니다.");
        }

        // 새 인증 토큰 생성 및 전송
        String verificationToken = emailService.generateVerificationToken(email);
        emailService.sendVerificationEmail(email, verificationToken);*/
    }

    @Override
    @Transactional(readOnly = true)
    public MyProfileResponseDto getMyProfile(Long userId) {
        User user = userRepository.findByIdWithProfileImage(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        if (user.isDeleted()) {
            throw InvalidUserInputException.of("탈퇴한 회원의 프로필은 조회할 수 없습니다.");
        }

        return MyProfileResponseDto.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getUserProfile(Long currentUserId, Long targetUserId) {

        User user = userRepository.findByIdWithProfileImage(targetUserId)
                .orElseThrow(() -> UserNotFoundException.of(targetUserId));

        if (user.isDeleted()) {
            throw InvalidUserInputException.of("탈퇴한 회원의 프로필은 조회할 수 없습니다.");
        }

        FriendStatus friendStatus = friendService.getFriendStatus(currentUserId, targetUserId);

        return UserProfileResponseDto.from(user, friendStatus);
    }

    @Override
    @Transactional
    public void updateChatSettings(String token, boolean randomChatEnabled) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        if (user.isDeleted()) {
            throw InvalidUserInputException.of("탈퇴한 회원은 설정을 변경할 수 없습니다.");
        }

        user.updateChatSettings(randomChatEnabled);
    }

    @Override
    @Transactional
    public void updateSecretStatus(String token, boolean isSecret) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        if (user.isDeleted()) {
            throw InvalidUserInputException.of("탈퇴한 회원은 설정을 변경할 수 없습니다.");
        }

        user.setSecretStatus(isSecret);
        notifyStatusChange(user);
    }

    private void notifyStatusChange(User user) {
        userStatusNotificationService.notifyFriendsStatusChange(user);
    }
}