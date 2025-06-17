package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInvitationResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatInvitationService;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat-invitations")
@RequiredArgsConstructor
@Tag(name = "Chat Invitations", description = "채팅방 초대 관리 API")
public class ChatInvitationController {

    private final ChatInvitationService chatInvitationService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/pending")
    @Operation(
            summary = "대기 중인 초대 목록 조회",
            description = "현재 사용자에게 온 대기 중인 채팅방 초대 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<ChatInvitationResponseDto>>> getPendingInvitations(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        log.debug("대기 중인 초대 목록 조회 요청 - 사용자: {}", userId);

        List<ChatInvitationResponseDto> invitations = chatInvitationService.getPendingInvitations(userId);

        log.debug("대기 중인 초대 목록 조회 완료 - 사용자: {}, 초대 수: {}", userId, invitations.size());
        return ResponseEntity.ok(ApiResponse.success("대기 중인 초대 목록을 조회했습니다.", invitations));
    }

    @GetMapping("/sent")
    @Operation(
            summary = "보낸 초대 목록 조회",
            description = "현재 사용자가 보낸 채팅방 초대 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<ChatInvitationResponseDto>>> getSentInvitations(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        log.debug("보낸 초대 목록 조회 요청 - 사용자: {}", userId);

        List<ChatInvitationResponseDto> invitations = chatInvitationService.getSentInvitations(userId);

        log.debug("보낸 초대 목록 조회 완료 - 사용자: {}, 초대 수: {}", userId, invitations.size());
        return ResponseEntity.ok(ApiResponse.success("보낸 초대 목록을 조회했습니다.", invitations));
    }

    @PostMapping("/{invitationId}/accept")
    @Operation(
            summary = "채팅방 초대 수락",
            description = "받은 채팅방 초대를 수락합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "초대 ID", required = true)
            @PathVariable Long invitationId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("채팅방 초대 수락 요청 - 사용자: {}, 초대 ID: {}", userId, invitationId);

        chatInvitationService.acceptInvitation(invitationId, userId);

        log.info("채팅방 초대 수락 완료 - 사용자: {}, 초대 ID: {}", userId, invitationId);
        return ResponseEntity.ok(ApiResponse.success("채팅방 초대를 수락했습니다."));
    }

    @PostMapping("/{invitationId}/reject")
    @Operation(
            summary = "채팅방 초대 거절",
            description = "받은 채팅방 초대를 거절합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "초대 ID", required = true)
            @PathVariable Long invitationId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("채팅방 초대 거절 요청 - 사용자: {}, 초대 ID: {}", userId, invitationId);

        chatInvitationService.rejectInvitation(invitationId, userId);

        log.info("채팅방 초대 거절 완료 - 사용자: {}, 초대 ID: {}", userId, invitationId);
        return ResponseEntity.ok(ApiResponse.success("채팅방 초대를 거절했습니다."));
    }

    @DeleteMapping("/{invitationId}")
    @Operation(
            summary = "초대 삭제",
            description = "보낸 초대를 취소하거나 받은 초대 기록을 삭제합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> deleteInvitation(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "초대 ID", required = true)
            @PathVariable Long invitationId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("초대 삭제 요청 - 사용자: {}, 초대 ID: {}", userId, invitationId);

        chatInvitationService.deleteInvitation(invitationId, userId);

        log.info("초대 삭제 완료 - 사용자: {}, 초대 ID: {}", userId, invitationId);
        return ResponseEntity.ok(ApiResponse.success("초대가 삭제되었습니다."));
    }

    private Long extractUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7); // "Bearer " 제거
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}