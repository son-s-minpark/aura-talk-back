package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "채팅 메시지 관련 API")
public class ChatController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    // 웹소켓으로 채팅 전송
    /*@PostMapping("/{chatroomId}")
    @Operation(
            summary = "채팅 내용 전송",
            description = "채팅방에 메시지를 전송합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatMessageResponseDto>> sendMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @Valid @RequestBody ChatMessageRequestDto requestDto) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        ChatMessageResponseDto responseDto = chatService.sendMessage(chatroomId, requestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("메시지가 전송되었습니다.", responseDto));
    }*/

    @GetMapping("/{chatroomId}")
    @Operation(
            summary = "채팅 목록 조회",
            description = "채팅방의 메시지 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Page<ChatMessageResponseDto>>> getMessages(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ChatMessageResponseDto> messages = chatService.getMessages(chatroomId, userId, pageable);

        return ResponseEntity.ok(ApiResponse.success("채팅 목록 조회 성공", messages));
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