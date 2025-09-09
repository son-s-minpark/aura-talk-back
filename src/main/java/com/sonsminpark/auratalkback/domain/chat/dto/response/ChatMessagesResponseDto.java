package com.sonsminpark.auratalkback.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessagesResponseDto {
    private List<ChatMessageResponseDto> messages;
    private boolean hasMore;          // 더 많은 메시지가 있는지
    private Long firstMessageId;     // 첫 번째 메시지 ID (가장 최신)
    private Long lastMessageId;      // 마지막 메시지 ID (가장 오래된)
    private int messageCount;        // 현재 응답의 메시지 개수
    private Long totalMessageCount;  // 채팅방 전체 메시지 개수
}