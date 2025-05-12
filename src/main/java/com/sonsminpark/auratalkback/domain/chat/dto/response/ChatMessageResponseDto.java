package com.sonsminpark.auratalkback.domain.chat.dto.response;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatMessage;
import com.sonsminpark.auratalkback.domain.chat.entity.MessageType;
import com.sonsminpark.auratalkback.domain.user.dto.response.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponseDto {

    private Long id;
    private Long chatRoomId;
    private UserResponseDto sender;
    private String content;
    private MessageType type;
    private LocalDateTime createdAt;
    private boolean isDeleted;

    public static ChatMessageResponseDto from(ChatMessage chatMessage) {
        return ChatMessageResponseDto.builder()
                .id(chatMessage.getId())
                .chatRoomId(chatMessage.getChatRoom().getId())
                .sender(UserResponseDto.from(chatMessage.getSender()))
                .content(chatMessage.getContent())
                .type(chatMessage.getType())
                .createdAt(chatMessage.getCreatedAt())
                .isDeleted(chatMessage.isDeleted())
                .build();
    }
}