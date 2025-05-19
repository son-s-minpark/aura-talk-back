package com.sonsminpark.auratalkback.domain.chat.dto.response;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatInvitation;
import com.sonsminpark.auratalkback.domain.chat.entity.InvitationStatus;
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
public class ChatInvitationResponseDto {

    private Long id;
    private Long chatRoomId;
    private String chatRoomName;
    private UserResponseDto inviter;
    private UserResponseDto invitee;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static ChatInvitationResponseDto from(ChatInvitation invitation) {
        return ChatInvitationResponseDto.builder()
                .id(invitation.getId())
                .chatRoomId(invitation.getChatRoom().getId())
                .chatRoomName(invitation.getChatRoom().getName())
                .inviter(UserResponseDto.from(invitation.getInviter()))
                .invitee(UserResponseDto.from(invitation.getInvitee()))
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .respondedAt(invitation.getRespondedAt())
                .build();
    }
}