package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.websocket.WebSocketUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{chatroomId}")
    @SendTo("/topic/chatroom/{chatroomId}")
    public ChatMessageResponseDto sendMessage(
            @DestinationVariable Long chatroomId,
            ChatMessageRequestDto message,
            Principal principal) {

        log.info("WebSocket message received for chatroom: {}", chatroomId);

        if (principal instanceof WebSocketUser webSocketUser) {
            Long userId = webSocketUser.getUserId();
            log.info("Processing message from user: {} for chatroom: {}", userId, chatroomId);

            return chatService.sendMessage(chatroomId, message, userId);
        } else {
            log.error("Invalid principal type: {}", principal != null ? principal.getClass() : "null");
            throw new RuntimeException("인증된 사용자가 아닙니다.");
        }
    }
}