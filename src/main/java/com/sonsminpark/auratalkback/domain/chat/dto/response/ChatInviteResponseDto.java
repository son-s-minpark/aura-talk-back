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
public class ChatInviteResponseDto {

    private String inviteCode;
    private String inviteLink;
    private LocalDateTime expiresAt;
}