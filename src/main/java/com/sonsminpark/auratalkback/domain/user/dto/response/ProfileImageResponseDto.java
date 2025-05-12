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
        return ProfileImageResponseDto.builder()
                .userId(userProfileImage.getUserId())
                .originalImageUrl(userProfileImage.getOriginalImageUrl())
                .thumbnailImageUrl(userProfileImage.getThumbnailImageUrl())
                .isDefaultProfileImage(userProfileImage.isDefaultProfileImage())
                .build();
    }
}
