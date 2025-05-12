package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    @MessageMapping("/chat/{chatroomId}")
    @SendTo("/topic/chatroom/{chatroomId}")
    public ChatMessageResponseDto sendMessage(
            @DestinationVariable Long chatroomId,
            @Header("Authorization") String authHeader,
            ChatMessageRequestDto message) {

        log.info("WebSocket message received for chatroom: {}", chatroomId);

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        return chatService.sendMessage(chatroomId, message, userId);
    }
}