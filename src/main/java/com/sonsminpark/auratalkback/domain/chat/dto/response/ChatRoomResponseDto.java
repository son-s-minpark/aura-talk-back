package com.sonsminpark.auratalkback.domain.chat.dto.response;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import com.sonsminpark.auratalkback.domain.user.dto.response.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponseDto {

    private Long id;
    private String name;
    private ChatRoomType type;
    private UserResponseDto owner;
    private List<UserResponseDto> users;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private boolean isActive;
    private String roomImageUrl;
    private boolean isOwner;
    private String inviteCode;
    private LocalDateTime inviteCodeExpiredAt;

    public static ChatRoomResponseDto from(ChatRoom chatRoom) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .type(chatRoom.getType())
                .owner(chatRoom.getOwner() != null ? UserResponseDto.from(chatRoom.getOwner()) : null)
                .users(chatRoom.getUsers().stream()
                        .map(UserResponseDto::from)
                        .collect(Collectors.toList()))
                .createdAt(chatRoom.getCreatedAt())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .isActive(chatRoom.isActive())
                .roomImageUrl(chatRoom.getRoomImageUrl())
                .inviteCode(chatRoom.getInviteCode())
                .inviteCodeExpiredAt(chatRoom.getInviteCodeExpiredAt())
                .build();
    }

    public static ChatRoomResponseDto from(ChatRoom chatRoom, Long currentUserId) {
        ChatRoomResponseDto dto = from(chatRoom);
        dto.isOwner = chatRoom.isUserOwner(currentUserId);
        return dto;
    }
}