package com.sonsminpark.auratalkback.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomImageResponseDto {

    private String originalImageUrl;
    private String thumbnailImageUrl;
    private boolean isDefaultImage;
}