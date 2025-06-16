package com.sonsminpark.auratalkback.domain.user.dto.response;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyProfileResponseDto {

    private Long id;
    private String email;
    private String username;
    private String nickname;
    private String description;
    private List<String> interests;
    private UserStatus status;
    private boolean randomChatEnabled;
    private LocalDateTime createdAt;
    private ProfileImageResponseDto profileImage;

    public static MyProfileResponseDto from(User user) {
        return MyProfileResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .description(user.getDescription())
                .interests(user.getInterests())
                .status(user.getStatus())
                .randomChatEnabled(user.isRandomChatEnabled())
                .createdAt(user.getCreatedAt())
                .profileImage(ProfileImageResponseDto.from(user.getUserProfileImage()))
                .build();
    }
}