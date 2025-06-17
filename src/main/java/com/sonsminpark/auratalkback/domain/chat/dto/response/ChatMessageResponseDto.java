package com.sonsminpark.auratalkback.domain.chat.dto.response;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatMessage;
import com.sonsminpark.auratalkback.domain.chat.entity.MessageType;
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
    private ChatUserResponseDto sender;
    private String content;
    private MessageType type;
    private LocalDateTime createdAt;
    private boolean isDeleted;

    public static ChatMessageResponseDto from(ChatMessage chatMessage) {
        return ChatMessageResponseDto.builder()
                .id(chatMessage.getId())
                .chatRoomId(chatMessage.getChatRoom().getId())
                .sender(chatMessage.getSender() != null ? ChatUserResponseDto.from(chatMessage.getSender()) : null)
                .content(chatMessage.getContent())
                .type(chatMessage.getType())
                .createdAt(chatMessage.getCreatedAt())
                .isDeleted(chatMessage.isDeleted())
                .build();
    }

    public static ChatMessageResponseDto from(ChatMessage chatMessage, String senderThumbnailUrl) {
        return ChatMessageResponseDto.builder()
                .id(chatMessage.getId())
                .chatRoomId(chatMessage.getChatRoom().getId())
                .sender(chatMessage.getSender() != null ?
                        ChatUserResponseDto.from(chatMessage.getSender(), senderThumbnailUrl) : null)
                .content(chatMessage.getContent())
                .type(chatMessage.getType())
                .createdAt(chatMessage.getCreatedAt())
                .isDeleted(chatMessage.isDeleted())
                .build();
    }
}