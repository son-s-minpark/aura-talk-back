package com.sonsminpark.auratalkback.domain.user.dto.response;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String status;
    private boolean isDeleted;
    private boolean emailVerified;
    private boolean randomChatEnabled;
    private ProfileImageResponseDto profileImage;

    public static MyProfileResponseDto from(User user) {
        return MyProfileResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .description(user.getDescription())
                .interests(user.getInterests())
                .status(user.getStatus().name())
                .isDeleted(user.isDeleted())
                .emailVerified(user.isEmailVerified())
                .randomChatEnabled(user.isRandomChatEnabled())
                .profileImage(ProfileImageResponseDto.from(user.getUserProfileImage())) // null이면 null 반환
                .build();
    }
}