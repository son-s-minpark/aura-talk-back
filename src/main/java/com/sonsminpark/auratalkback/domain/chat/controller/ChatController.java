package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessagesResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "채팅 메시지 관련 API")
public class ChatController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/{chatroomId}")
    @Operation(
            summary = "채팅 메시지 조회",
            description = "채팅방의 메시지 목록을 무한 스크롤 방식으로 조회합니다. " +
                    "beforeMessageId가 없으면 최신 메시지부터, 있으면 해당 메시지 이전의 메시지들을 반환합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatMessagesResponseDto>> getMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @Parameter(description = "이전 메시지 로드용 기준 메시지 ID (없으면 최신 메시지부터)")
            @RequestParam(required = false) Long beforeMessageId,
            @Parameter(description = "새 메시지 로드용 기준 메시지 ID")
            @RequestParam(required = false) Long afterMessageId,
            @Parameter(description = "메시지 개수 (최대 100)", example = "50")
            @RequestParam(defaultValue = "50") int limit) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        if (limit > 100) {
            limit = 100;
            log.debug("메시지 개수를 최대값 100으로 조정 - 사용자: {}, 채팅방: {}", userId, chatroomId);
        }
        if (limit < 1) {
            limit = 1;
        }

        try {
            ChatMessagesResponseDto messages;

            if (afterMessageId != null) {
                // 새로운 메시지 로드
                log.debug("새 메시지 조회 시작 - 채팅방: {}, 사용자: {}, afterMessageId: {}, limit: {}",
                        chatroomId, userId, afterMessageId, limit);
                messages = chatService.getMessagesAfter(chatroomId, userId, afterMessageId, limit);
            } else {
                // 이전 메시지 로드 또는 최신 메시지 로드
                log.debug("이전 메시지 조회 시작 - 채팅방: {}, 사용자: {}, beforeMessageId: {}, limit: {}",
                        chatroomId, userId, beforeMessageId, limit);
                messages = chatService.getMessagesBefore(chatroomId, userId, beforeMessageId, limit);
            }

            log.debug("메시지 조회 완료 - 채팅방: {}, 사용자: {}, 조회된 메시지: {}, hasMore: {}",
                    chatroomId, userId, messages.getMessages().size(), messages.isHasMore());

            return ResponseEntity.ok(ApiResponse.success("채팅 메시지 조회 성공", messages));

        } catch (Exception e) {
            log.error("채팅 메시지 조회 실패 - 채팅방: {}, 사용자: {}, 오류: {}",
                    chatroomId, userId, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{messageId}")
    @Operation(
            summary = "채팅 삭제",
            description = "본인이 보낸 메시지를 삭제합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long messageId) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        chatService.deleteMessage(messageId, userId);
        return ResponseEntity.ok(ApiResponse.success("메시지가 삭제되었습니다."));
    }
}