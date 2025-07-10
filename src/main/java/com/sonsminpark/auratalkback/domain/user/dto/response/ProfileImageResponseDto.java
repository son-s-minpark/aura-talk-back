package com.sonsminpark.auratalkback.domain.user.dto.response;

import com.sonsminpark.auratalkback.domain.user.entity.UserProfileImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileImageResponseDto {

    private Long userId;
    private String originalImageUrl;
    private String thumbnailImageUrl;
    private boolean isDefaultProfileImage;

    public static ProfileImageResponseDto from(UserProfileImage userProfileImage) {
        if (userProfileImage == null) {
            return null;
        }

        return ProfileImageResponseDto.builder()
                .userId(userProfileImage.getUserId())
                .originalImageUrl(userProfileImage.getOriginalImageUrl())
                .thumbnailImageUrl(userProfileImage.getThumbnailImageUrl())
                .isDefaultProfileImage(userProfileImage.isDefaultProfileImage())
                .build();
    }

    public static ProfileImageResponseDto createDefault(Long userId) {
        int index = Math.toIntExact(userId % 4) + 1;
        String bucketUrl = "https://your-bucket.s3.amazonaws.com";

        return ProfileImageResponseDto.builder()
                .userId(userId)
                .originalImageUrl(bucketUrl + "/profile-images/default/" + index + ".png")
                .thumbnailImageUrl(bucketUrl + "/profile-images/default/" + index + "_thumb.png")
                .isDefaultProfileImage(true)
                .build();
    }
}