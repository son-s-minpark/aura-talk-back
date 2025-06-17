package com.sonsminpark.auratalkback.domain.chat.dto.response;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatInvitation;
import com.sonsminpark.auratalkback.domain.chat.entity.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatInvitationResponseDto {

    private Long id;
    private Long chatRoomId;
    private String chatRoomName;
    private ChatUserResponseDto inviter;
    private ChatUserResponseDto invitee;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static ChatInvitationResponseDto from(ChatInvitation invitation) {
        return ChatInvitationResponseDto.builder()
                .id(invitation.getId())
                .chatRoomId(invitation.getChatRoom().getId())
                .chatRoomName(invitation.getChatRoom().getName())
                .inviter(ChatUserResponseDto.from(invitation.getInviter()))
                .invitee(ChatUserResponseDto.from(invitation.getInvitee()))
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }

    public void setInviterThumbnailUrl(String thumbnailImageUrl) {
        if (this.inviter != null) {
            this.inviter.setThumbnailImageUrl(thumbnailImageUrl);
        }
    }

    public void setInviteeThumbnailUrl(String thumbnailImageUrl) {
        if (this.invitee != null) {
            this.invitee.setThumbnailImageUrl(thumbnailImageUrl);
        }
    }
}