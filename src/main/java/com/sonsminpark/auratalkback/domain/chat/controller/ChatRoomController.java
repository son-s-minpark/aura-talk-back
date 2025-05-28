package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInviteResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInvitationResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
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
            description = "새로운 채팅방을 생성합니다. 초대할 사용자 목록을 포함할 수 있습니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createChatRoom(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChatRoomCreateRequestDto requestDto) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("채팅방 생성 요청 - 사용자: {}, 채팅방명: {}", userId, requestDto.getName());

        ChatRoomResponseDto responseDto = chatService.createChatRoom(requestDto, userId);

        log.info("채팅방 생성 완료 - ID: {}, 이름: {}", responseDto.getId(), responseDto.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("채팅방이 성공적으로 생성되었습니다.", responseDto));
    }

    @GetMapping
    @Operation(
            summary = "채팅방 목록 조회",
            description = "현재 사용자가 참여중인 채팅방 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> getChatRooms(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        log.debug("채팅방 목록 조회 요청 - 사용자: {}", userId);

        List<ChatRoomResponseDto> chatRooms = chatService.getChatRoomsByUserId(userId);

        log.debug("채팅방 목록 조회 완료 - 사용자: {}, 채팅방 수: {}", userId, chatRooms.size());
        return ResponseEntity.ok(ApiResponse.success("채팅방 목록을 성공적으로 조회했습니다.", chatRooms));
    }

    @DeleteMapping("/{chatroomId}")
    @Operation(
            summary = "채팅방 나가기",
            description = "사용자가 채팅방을 나갑니다. 방장이 나가면 채팅방이 비활성화됩니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("채팅방 나가기 요청 - 사용자: {}, 채팅방: {}", userId, chatroomId);

        chatService.leaveChatRoom(chatroomId, userId);

        log.info("채팅방 나가기 완료 - 사용자: {}, 채팅방: {}", userId, chatroomId);
        return ResponseEntity.ok(ApiResponse.success("채팅방을 나갔습니다."));
    }

    @PostMapping("/{chatroomId}/invite")
    @Operation(
            summary = "친구 초대하기",
            description = "채팅방에 친구를 초대합니다. 초대받은 사용자는 수락/거절할 수 있습니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatInvitationResponseDto>> inviteUser(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId,
            @Valid @RequestBody ChatInviteRequestDto requestDto) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("사용자 초대 요청 - 초대자: {}, 채팅방: {}, 피초대자: {}",
                userId, chatroomId, requestDto.getUserId());

        ChatInvitationResponseDto invitation = chatService.inviteUser(chatroomId, requestDto, userId);

        log.info("사용자 초대 완료 - 초대 ID: {}", invitation.getId());
        return ResponseEntity.ok(ApiResponse.success("초대 요청을 전송했습니다.", invitation));
    }

    @PostMapping("/{chatroomId}/invite-link")
    @Operation(
            summary = "초대 링크 생성",
            description = "채팅방 초대 링크를 생성합니다. 방장만 생성할 수 있으며 24시간 후 만료됩니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatInviteResponseDto>> createInviteLink(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("초대 링크 생성 요청 - 사용자: {}, 채팅방: {}", userId, chatroomId);

        ChatInviteResponseDto responseDto = chatService.createInviteLink(chatroomId, userId);

        log.info("초대 링크 생성 완료 - 채팅방: {}, 만료시간: {}", chatroomId, responseDto.getExpiresAt());
        return ResponseEntity.ok(ApiResponse.success("초대 링크가 생성되었습니다.", responseDto));
    }

    @GetMapping("/invitations/pending")
    @Operation(
            summary = "대기중인 초대 조회",
            description = "현재 사용자에게 온 대기중인 초대 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<ChatInvitationResponseDto>>> getPendingInvitations(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        log.debug("대기중인 초대 조회 요청 - 사용자: {}", userId);

        List<ChatInvitationResponseDto> invitations = chatService.getPendingInvitations(userId);

        log.debug("대기중인 초대 조회 완료 - 사용자: {}, 초대 수: {}", userId, invitations.size());
        return ResponseEntity.ok(ApiResponse.success("대기중인 초대 목록을 조회했습니다.", invitations));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @Operation(
            summary = "초대 수락",
            description = "채팅방 초대를 수락합니다. 수락 시 해당 채팅방에 참여됩니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "초대 ID", required = true)
            @PathVariable Long invitationId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("초대 수락 요청 - 사용자: {}, 초대 ID: {}", userId, invitationId);

        chatService.acceptInvitation(invitationId, userId);

        log.info("초대 수락 완료 - 사용자: {}, 초대 ID: {}", userId, invitationId);
        return ResponseEntity.ok(ApiResponse.success("초대를 수락했습니다."));
    }

    @PostMapping("/invitations/{invitationId}/reject")
    @Operation(
            summary = "초대 거절",
            description = "채팅방 초대를 거절합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "초대 ID", required = true)
            @PathVariable Long invitationId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("초대 거절 요청 - 사용자: {}, 초대 ID: {}", userId, invitationId);

        chatService.rejectInvitation(invitationId, userId);

        log.info("초대 거절 완료 - 사용자: {}, 초대 ID: {}", userId, invitationId);
        return ResponseEntity.ok(ApiResponse.success("초대를 거절했습니다."));
    }

    @PostMapping("/invite/accept")
    @Operation(
            summary = "초대 링크 수락",
            description = "초대 링크를 통한 채팅방 참여를 수락합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> acceptInviteLink(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "초대 코드", required = true)
            @RequestParam @NotBlank(message = "초대 코드는 필수입니다.") String inviteCode) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("초대 링크 수락 요청 - 사용자: {}, 초대 코드: {}", userId, inviteCode);

        chatService.acceptInvite(inviteCode, userId);

        log.info("초대 링크 수락 완료 - 사용자: {}", userId);
        return ResponseEntity.ok(ApiResponse.success("초대를 수락했습니다."));
    }

    @PostMapping("/invite/reject")
    @Operation(
            summary = "초대 링크 거절",
            description = "초대 링크를 통한 채팅방 참여를 거절합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> rejectInviteLink(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "초대 코드", required = true)
            @RequestParam @NotBlank(message = "초대 코드는 필수입니다.") String inviteCode) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("초대 링크 거절 요청 - 사용자: {}, 초대 코드: {}", userId, inviteCode);

        chatService.rejectInvite(inviteCode, userId);

        log.info("초대 링크 거절 완료 - 사용자: {}", userId);
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
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId,
            @Parameter(description = "알림 활성화 여부", required = true)
            @RequestParam boolean enabled) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("알림 설정 변경 요청 - 사용자: {}, 채팅방: {}, 활성화: {}", userId, chatroomId, enabled);

        chatService.updateNotificationSettings(chatroomId, userId, enabled);

        log.info("알림 설정 변경 완료 - 사용자: {}, 채팅방: {}", userId, chatroomId);
        return ResponseEntity.ok(ApiResponse.success("알림 설정이 변경되었습니다."));
    }

    private Long extractUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7); // "Bearer " 제거
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}