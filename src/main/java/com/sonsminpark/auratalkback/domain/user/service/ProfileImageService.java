package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.response.ProfileImageResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProfileImageService{

    ProfileImageResponseDto uploadProfileImage(Long userId, MultipartFile file) throws IOException;

    ProfileImageResponseDto getProfileImageUrl(Long userId);
}