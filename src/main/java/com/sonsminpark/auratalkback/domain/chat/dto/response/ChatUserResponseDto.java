package com.sonsminpark.auratalkback.domain.chat.dto.response;

import com.sonsminpark.auratalkback.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatUserResponseDto {

    private Long id;
    private String nickname;
    private String thumbnailImageUrl;

    public static ChatUserResponseDto from(User user) {
        return ChatUserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .thumbnailImageUrl(null) // 프로필 이미지는 별도 서비스에서 설정
                .build();
    }

    public static ChatUserResponseDto from(User user, String thumbnailImageUrl) {
        return ChatUserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .thumbnailImageUrl(thumbnailImageUrl)
                .build();
    }

    public void setThumbnailImageUrl(String thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }
}