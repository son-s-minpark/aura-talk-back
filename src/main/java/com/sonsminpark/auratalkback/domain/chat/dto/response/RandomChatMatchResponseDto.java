package com.sonsminpark.auratalkback.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RandomChatMatchResponseDto {
    private boolean matched;
    private boolean waiting;
    private ChatRoomResponseDto chatRoom;
    private ChatUserResponseDto matchedUser;
    private LocalDateTime matchedAt;
    private String message;
}