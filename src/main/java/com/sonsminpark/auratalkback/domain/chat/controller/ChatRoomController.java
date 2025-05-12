package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInviteResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
@Tag(name = "ChatRoom", description = "채팅방 관련 API")
public class ChatRoomController {

    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    @Operation(
            summary = "채팅방 만들기",
            description = "새로운 채팅방을 생성합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createChatRoom(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChatRoomCreateRequestDto requestDto) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        ChatRoomResponseDto responseDto = chatService.createChatRoom(requestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("채팅방이 생성되었습니다.", responseDto));
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "채팅방 목록 조회",
            description = "사용자가 참여중인 채팅방 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> getChatRoomsByUserId(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long userId) {

        String token = authHeader.substring(7);
        Long tokenUserId = jwtTokenProvider.getUserIdFromToken(token);

        // 본인의 채팅방 목록만 조회 가능
        if (!tokenUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(com.sonsminpark.auratalkback.global.exception.ErrorCode.ACCESS_DENIED));
        }

        List<ChatRoomResponseDto> chatRooms = chatService.getChatRoomsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("채팅방 목록 조회 성공", chatRooms));
    }

    @DeleteMapping("/{chatroomId}")
    @Operation(
            summary = "채팅방 나가기",
            description = "사용자가 채팅방을 나갑니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        chatService.leaveChatRoom(chatroomId, userId);
        return ResponseEntity.ok(ApiResponse.success("채팅방을 나갔습니다."));
    }

    @PostMapping("/{chatroomId}/invite")
    @Operation(
            summary = "친구 초대하기",
            description = "채팅방에 친구를 초대합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> inviteUser(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @Valid @RequestBody ChatInviteRequestDto requestDto) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        chatService.inviteUser(chatroomId, requestDto, userId);
        return ResponseEntity.ok(ApiResponse.success("초대 요청을 전송했습니다."));
    }

    @PostMapping("/{chatroomId}/invite-link")
    @Operation(
            summary = "초대 링크 생성",
            description = "채팅방 초대 링크를 생성합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatInviteResponseDto>> createInviteLink(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        ChatInviteResponseDto responseDto = chatService.createInviteLink(chatroomId, userId);
        return ResponseEntity.ok(ApiResponse.success("초대 링크가 생성되었습니다.", responseDto));
    }

    @PostMapping("/invite/accept")
    @Operation(
            summary = "초대 수락",
            description = "채팅방 초대를 수락합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> acceptInvite(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String inviteCode) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        chatService.acceptInvite(inviteCode, userId);
        return ResponseEntity.ok(ApiResponse.success("초대를 수락했습니다."));
    }

    @PostMapping("/invite/reject")
    @Operation(
            summary = "초대 거절",
            description = "채팅방 초대를 거절합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> rejectInvite(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String inviteCode) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        chatService.rejectInvite(inviteCode, userId);
        return ResponseEntity.ok(ApiResponse.success("초대를 거절했습니다."));
    }

    @PutMapping("/{chatroomId}/notification")
    @Operation(
            summary = "채팅방 알림 설정",
            description = "채팅방의 알림 설정을 변경합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> updateNotificationSettings(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @RequestParam boolean enabled) {

        String token = authHeader.substring(7);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        chatService.updateNotificationSettings(chatroomId, userId, enabled);
        return ResponseEntity.ok(ApiResponse.success("알림 설정이 변경되었습니다."));
    }
}