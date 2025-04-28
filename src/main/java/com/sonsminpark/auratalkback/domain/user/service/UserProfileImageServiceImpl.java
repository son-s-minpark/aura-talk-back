package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.response.ProfileImageResponseDto;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserProfileImage;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserProfileImageRepository;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

@RequiredArgsConstructor
@Service
public class UserProfileImageServiceImpl implements UserProfileImageService {

    private final S3Client s3Client;
    private final S3Service s3Service;
    private final UserProfileImageRepository userProfileImageRepository;
    private final UserRepository userRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final int DEFAULT_IMAGE_COUNT = 4;
    private static record DefaultProfileImage(String originalUrl, String thumbnailUrl) {}

    private DefaultProfileImage getDefaultProfileImage(Long userId) {
        int index = Math.toIntExact(userId % DEFAULT_IMAGE_COUNT) + 1;
        String prefix = "https://" + bucketName + ".s3.amazonaws.com/profile-images/default/";
        return new DefaultProfileImage(
                prefix + index + ".png",
                prefix + index + "_thumb.png"
        );
    }

    @Override
    @Transactional
    public ProfileImageResponseDto createDefaultProfileIamge(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        DefaultProfileImage defaultProfileImage = getDefaultProfileImage(userId);
        UserProfileImage profileImage = UserProfileImage.builder()
                .user(user)
                .originalImageUrl(defaultProfileImage.originalUrl())
                .thumbnailImageUrl(defaultProfileImage.thumbnailUrl())
                .isDefaultProfileImage(true)
                .build();

        userProfileImageRepository.save(profileImage);

        return ProfileImageResponseDto.from(profileImage);
    }

    @Override
    @Transactional
    public ProfileImageResponseDto updateProfileImage(Long userId, String s3Key) {

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        UserProfileImage profileImage = userProfileImageRepository.findByUserId(userId)
                .orElse(new UserProfileImage(user));

        if (!profileImage.isDefaultProfileImage()) {
            s3Service.deleteFileFromS3(profileImage.getOriginalImageUrl());
            s3Service.deleteFileFromS3(profileImage.getThumbnailImageUrl());
        }

        String originalImageUrl = "https://" + bucketName + ".s3.amazonaws.com/" + s3Key;
        String thumbnailImageUrl = originalImageUrl.replace("/original/", "/thumbnail/");

        profileImage.updateImage(originalImageUrl, thumbnailImageUrl, false);
        userProfileImageRepository.save(profileImage);

        return ProfileImageResponseDto.from(profileImage);

    }

    @Override
    @Transactional
    public ProfileImageResponseDto getProfileImage(Long userId) {

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        UserProfileImage profileImage = userProfileImageRepository.findByUserId(userId)
                .orElseGet(() -> {
                    DefaultProfileImage defaultImage = getDefaultProfileImage(userId);
                    UserProfileImage newImage = new UserProfileImage(user);
                    newImage.updateImage(defaultImage.originalUrl(), defaultImage.thumbnailUrl(), true);
                    return userProfileImageRepository.save(newImage);
                });

        ProfileImageResponseDto profileImageResponseDto = ProfileImageResponseDto.from(profileImage);

        return profileImageResponseDto;

    }

    @Override
    @Transactional
    public ProfileImageResponseDto deleteProfileImage(Long userId) {

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        DefaultProfileImage defaultImage = getDefaultProfileImage(userId);

        UserProfileImage profileImage = userProfileImageRepository.findByUserId(userId)
                .orElse(new UserProfileImage(user));

        if (!profileImage.isDefaultProfileImage()) {
            s3Service.deleteFileFromS3(profileImage.getOriginalImageUrl());
            s3Service.deleteFileFromS3(profileImage.getThumbnailImageUrl());
        }

        profileImage.updateImage(defaultImage.originalUrl(), defaultImage.thumbnailUrl(), true);
        userProfileImageRepository.save(profileImage);

        return ProfileImageResponseDto.from(profileImage);
    }


}
