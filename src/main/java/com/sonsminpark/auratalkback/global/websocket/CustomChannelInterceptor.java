package com.sonsminpark.auratalkback.global.websocket;

import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        String email = jwtTokenProvider.getEmailFromToken(token);
                        Long userId = jwtTokenProvider.getUserIdFromToken(token);
                        accessor.setUser(new WebSocketUser(email, userId));
                        log.info("WebSocket authentication successful for user: {}", email);
                    } else {
                        log.warn("Invalid JWT token in WebSocket connection");
                    }
                } catch (Exception e) {
                    log.error("Error processing JWT token in WebSocket: {}", e.getMessage());
                }
            }
        } else if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            // 메시지 전송 시에도 인증 확인
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        String email = jwtTokenProvider.getEmailFromToken(token);
                        Long userId = jwtTokenProvider.getUserIdFromToken(token);
                        accessor.setUser(new WebSocketUser(email, userId));
                    }
                } catch (Exception e) {
                    log.error("Error processing JWT token in WebSocket message: {}", e.getMessage());
                }
            }
        }

        return message;
    }
}