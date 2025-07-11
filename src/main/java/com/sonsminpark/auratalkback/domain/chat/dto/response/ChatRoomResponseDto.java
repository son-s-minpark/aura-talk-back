package com.sonsminpark.auratalkback.domain.chat.dto.response;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
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
public class ChatRoomResponseDto {

    private Long id;
    private String name;
    private ChatRoomType type;
    private ChatUserResponseDto owner;
    private List<ChatUserResponseDto> users;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private boolean isActive;
    private String roomImageUrl;
    private boolean isOwner;
    private String inviteCode;
    private LocalDateTime inviteCodeExpiredAt;

    public boolean isOwner() {
        return this.isOwner;
    }

    public static ChatRoomResponseDto from(ChatRoom chatRoom) {
        return ChatRoomResponseDto.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .type(chatRoom.getType())
                .owner(chatRoom.getOwner() != null ? ChatUserResponseDto.from(chatRoom.getOwner()) : null)
                .users(List.of())
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

    public void setUsers(List<ChatUserResponseDto> users) {
        this.users = users;
    }

    public void setOwnerThumbnailUrl(String thumbnailImageUrl) {
        if (this.owner != null) {
            this.owner.setThumbnailImageUrl(thumbnailImageUrl);
        }
    }
}