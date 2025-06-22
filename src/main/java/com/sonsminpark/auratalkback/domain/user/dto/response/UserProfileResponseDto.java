package com.sonsminpark.auratalkback.domain.user.dto.response;

import com.sonsminpark.auratalkback.domain.friend.entity.FriendStatus;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {

    private Long id;
    private FriendStatus friendStatus;
    private String username;
    private String nickname;
    private String description;
    private List<String> interests;
    private UserStatus status;
    private ProfileImageResponseDto profileImage;

    public static UserProfileResponseDto from(User user, FriendStatus friendStatus) {
        return UserProfileResponseDto.builder()
                .id(user.getId())
                .friendStatus(friendStatus)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .description(user.getDescription())
                .interests(user.getInterests())
                .status(user.getStatus())
                .profileImage(ProfileImageResponseDto.from(user.getUserProfileImage()))
                .build();
    }
}
