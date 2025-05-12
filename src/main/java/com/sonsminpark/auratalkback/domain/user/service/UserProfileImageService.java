package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.response.ProfileImageResponseDto;

public interface UserProfileImageService {

    ProfileImageResponseDto createDefaultProfileImage(Long userId);

    ProfileImageResponseDto getProfileImage(Long userId);

    ProfileImageResponseDto updateProfileImage(Long userId, String s3Key);

    ProfileImageResponseDto deleteProfileImage(Long userId);
}
