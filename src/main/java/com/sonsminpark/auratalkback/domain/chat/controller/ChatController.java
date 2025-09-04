package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
            summary = "채팅 목록 조회",
            description = "채팅방의 메시지 목록을 조회합니다. 최신 메시지부터 내림차순으로 정렬됩니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Page<ChatMessageResponseDto>>> getMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)", example = "50")
            @RequestParam(defaultValue = "50") int size) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        if (size > 100) {
            size = 100;
            log.debug("페이지 크기를 최대값 100으로 조정 - 사용자: {}, 채팅방: {}", userId, chatroomId);
        }
        if (size < 1) {
            size = 1;
        }
        if (page < 0) {
            page = 0;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        try {
            log.debug("채팅 메시지 조회 시작 - 채팅방: {}, 사용자: {}, 페이지: {}, 크기: {}",
                    chatroomId, userId, page, size);

            Page<ChatMessageResponseDto> messages = chatService.getMessages(chatroomId, userId, pageable);

            log.debug("채팅 메시지 조회 완료 - 채팅방: {}, 사용자: {}, 페이지: {}/{}, 조회된 메시지: {}, 총 메시지: {}",
                    chatroomId, userId, page + 1, messages.getTotalPages(),
                    messages.getNumberOfElements(), messages.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success("채팅 목록 조회 성공", messages));

        } catch (Exception e) {
            log.error("채팅 메시지 조회 실패 - 채팅방: {}, 사용자: {}, 페이지: {}, 오류: {}",
                    chatroomId, userId, page, e.getMessage(), e);
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