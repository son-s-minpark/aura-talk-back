package com.sonsminpark.auratalkback.domain.user.service;

import com.sonsminpark.auratalkback.domain.user.dto.response.ProfileImageResponseDto;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserProfileImage;
import com.sonsminpark.auratalkback.domain.user.exception.ImageSizeLimitExceededException;
import com.sonsminpark.auratalkback.domain.user.exception.ProfileImageNotFoundException;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserProfileImageRepository;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ProfileImageServiceImpl implements ProfileImageService {

    private final S3Client s3Client;
    private final UserProfileImageRepository userProfileImageRepository;
    private final UserRepository userRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Override
    @Transactional
    public ProfileImageResponseDto uploadProfileImage(Long userId, MultipartFile file) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.of(userId));

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB 제한
            throw ImageSizeLimitExceededException.of("이미지 크기가 너무 큽니다. 10MB 이하 이미지만 업로드할 수 있습니다.");
        }

        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("profile-images/original/" + fileName)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(
                putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        String profileImageUrl = "https://" + bucketName + ".s3.amazonaws.com/profile-images/original/" + fileName;

        Optional<UserProfileImage> userProfileImageOptional = userProfileImageRepository.findById(userId);

        if (userProfileImageOptional.isPresent()) {
            UserProfileImage existingImage = userProfileImageOptional.get();
            existingImage.updateProfileImage(profileImageUrl);
        }
        else {
            UserProfileImage newProfileImage = UserProfileImage.builder()
                    .user(user)
                    .imageUrl(profileImageUrl)
                    .build();
            userProfileImageRepository.save(newProfileImage);
        }

        ProfileImageResponseDto profileImageResponseDto = ProfileImageResponseDto.builder()
                .userId(userId)
                .imageUrl(profileImageUrl)
                .build();

        return profileImageResponseDto;
    }

    @Override
    public ProfileImageResponseDto getProfileImageUrl(Long userId) {
        UserProfileImage profileImage = userProfileImageRepository.findByUserId(userId)
                .orElseThrow(() -> ProfileImageNotFoundException.of("사용자 ID: " + userId + "의 프로필 이미지를 찾을 수 없습니다."));

        String profileImageUrl = profileImage.getImageUrl();

        ProfileImageResponseDto profileImageResponseDto = ProfileImageResponseDto.builder()
                .userId(userId)
                .imageUrl(profileImageUrl)
                .build();

        return profileImageResponseDto;

    }

}