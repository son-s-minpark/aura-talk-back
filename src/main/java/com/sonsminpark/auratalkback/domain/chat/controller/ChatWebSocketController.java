package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.websocket.WebSocketUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{chatroomId}")
    @SendTo("/topic/chatroom/{chatroomId}")
    public ChatMessageResponseDto sendMessage(
            @DestinationVariable Long chatroomId,
            ChatMessageRequestDto message,
            Principal principal) {

        log.debug("WebSocket 메시지 수신 - 채팅방: {}", chatroomId);

        if (!(principal instanceof WebSocketUser webSocketUser)) {
            log.error("유효하지 않은 Principal 타입: {}", principal != null ? principal.getClass() : "null");
            throw new IllegalArgumentException("인증된 사용자가 아닙니다.");
        }

        Long userId = webSocketUser.getUserId();
        log.debug("WebSocket 메시지 처리 - 사용자: {}, 채팅방: {}", userId, chatroomId);

        try {
            ChatMessageResponseDto response = chatService.sendMessage(chatroomId, message, userId);
            log.debug("WebSocket 메시지 전송 완료 - 메시지 ID: {}", response.getId());
            return response;
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패 - 사용자: {}, 채팅방: {}, 오류: {}",
                    userId, chatroomId, e.getMessage());
            throw e;
        }
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Exception e, Principal principal) {
        log.error("WebSocket 오류 발생: {}", e.getMessage(), e);

        String userId = "unknown";
        if (principal instanceof WebSocketUser webSocketUser) {
            userId = webSocketUser.getUserId().toString();
        }

        String errorMessage = "메시지 전송에 실패했습니다: " + e.getMessage();

        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/errors",
                    errorMessage
            );
        }

        return errorMessage;
    }

    private void sendErrorToUser(String userId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/errors",
                errorMessage
        );
    }
}