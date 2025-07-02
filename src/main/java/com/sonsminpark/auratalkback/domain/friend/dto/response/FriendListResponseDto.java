package com.sonsminpark.auratalkback.domain.friend.dto.response;

import com.sonsminpark.auratalkback.domain.friend.entity.FriendStatus;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendListResponseDto {

    private Long friendUserId;
    private FriendStatus friendStatus;
    private String username;
    private String nickname;
    private String description;
    private String thumbnailImageUrl;
    private UserStatus status;


    public static FriendListResponseDto from(User user, FriendStatus friendStatus) {
        return FriendListResponseDto.builder()
                .friendUserId(user.getId())
                .friendStatus(friendStatus)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .description(user.getDescription())
                .thumbnailImageUrl(user.getUserProfileImage().getThumbnailImageUrl())
                .status(user.getStatus().toPublicStatus())
                .build();
    }
}
