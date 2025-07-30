package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomUpdateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.OneToOneChatRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatInviteResponseDto;
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
            description = "새로운 채팅방을 생성합니다.",
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

    @PostMapping("/one-to-one")
    @Operation(
            summary = "1:1 채팅방 생성",
            description = "특정 사용자와의 1:1 채팅방을 생성하거나 기존 채팅방을 반환합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createOneToOneChatRoom(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody OneToOneChatRequestDto requestDto) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("1:1 채팅방 생성 요청 - 사용자: {}, 상대방: {}", userId, requestDto.getTargetUserId());

        ChatRoomResponseDto responseDto = chatService.createOneToOneChatRoom(userId, requestDto.getTargetUserId());

        log.info("1:1 채팅방 처리 완료 - ID: {}", responseDto.getId());
        return ResponseEntity.ok(ApiResponse.success("1:1 채팅방이 준비되었습니다.", responseDto));
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

    @GetMapping("/{chatroomId}")
    @Operation(
            summary = "채팅방 정보 조회",
            description = "특정 채팅방의 상세 정보를 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getChatRoomInfo(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.debug("채팅방 정보 조회 요청 - 사용자: {}, 채팅방: {}", userId, chatroomId);

        ChatRoomResponseDto chatRoom = chatService.getChatRoomInfo(chatroomId, userId);

        log.debug("채팅방 정보 조회 완료 - 채팅방: {}", chatroomId);
        return ResponseEntity.ok(ApiResponse.success("채팅방 정보를 성공적으로 조회했습니다.", chatRoom));
    }

    @PutMapping("/{chatroomId}")
    @Operation(
            summary = "채팅방 정보 수정",
            description = "채팅방의 이름이나 이미지를 수정합니다. 방장만 수정할 수 있습니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> updateChatRoom(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId,
            @Valid @RequestBody ChatRoomUpdateRequestDto requestDto) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("채팅방 정보 수정 요청 - 사용자: {}, 채팅방: {}", userId, chatroomId);

        ChatRoomResponseDto responseDto = chatService.updateChatRoom(chatroomId, requestDto, userId);

        log.info("채팅방 정보 수정 완료 - 채팅방: {}", chatroomId);
        return ResponseEntity.ok(ApiResponse.success("채팅방 정보가 성공적으로 수정되었습니다.", responseDto));
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
            summary = "친구에게 초대 링크 전송",
            description = "특정 친구에게 채팅방 초대 링크를 전송합니다. 링크를 받은 친구가 수락하면 채팅방에 참여됩니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatInviteResponseDto>> sendInviteToFriend(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId,
            @Valid @RequestBody ChatInviteRequestDto requestDto) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("친구 초대 링크 전송 요청 - 초대자: {}, 채팅방: {}, 피초대자: {}",
                userId, chatroomId, requestDto.getUserId());

        ChatInviteResponseDto invitation = chatService.sendInviteToFriend(chatroomId, requestDto, userId);

        log.info("친구 초대 링크 전송 완료 - 피초대자: {}", requestDto.getUserId());
        return ResponseEntity.ok(ApiResponse.success("친구에게 초대 링크를 전송했습니다.", invitation));
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

    @PostMapping("/join")
    @Operation(
            summary = "초대 링크로 채팅방 참여",
            description = "초대 링크를 통해 채팅방에 참여합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> joinChatRoomByInviteLink(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "초대 코드", required = true)
            @RequestParam @NotBlank(message = "초대 코드는 필수입니다.") String inviteCode) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("초대 링크로 채팅방 참여 요청 - 사용자: {}, 초대 코드: {}", userId, inviteCode);

        chatService.acceptInvite(inviteCode, userId);

        log.info("초대 링크로 채팅방 참여 완료 - 사용자: {}", userId);
        return ResponseEntity.ok(ApiResponse.success("채팅방에 참여했습니다."));
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

    @DeleteMapping("/{chatroomId}/delete")
    @Operation(
            summary = "채팅방 삭제",
            description = "방장이 채팅방을 완전히 삭제합니다. 모든 메시지와 사용자가 제거됩니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> deleteChatRoom(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("채팅방 삭제 요청 - 사용자: {}, 채팅방: {}", userId, chatroomId);

        chatService.deleteChatRoom(chatroomId, userId);

        log.info("채팅방 삭제 완료 - 채팅방: {}", chatroomId);
        return ResponseEntity.ok(ApiResponse.success("채팅방이 삭제되었습니다."));
    }

    @DeleteMapping("/{chatroomId}/kick/{targetUserId}")
    @Operation(
            summary = "채팅방 사용자 강퇴",
            description = "방장이 채팅방에서 특정 사용자를 강퇴합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> kickUser(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId,
            @Parameter(description = "강퇴할 사용자 ID", required = true)
            @PathVariable Long targetUserId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("사용자 강퇴 요청 - 방장: {}, 채팅방: {}, 대상: {}", userId, chatroomId, targetUserId);

        chatService.kickUser(chatroomId, userId, targetUserId);

        log.info("사용자 강퇴 완료 - 대상: {}, 채팅방: {}", targetUserId, chatroomId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 강퇴되었습니다."));
    }

    @PutMapping("/{chatroomId}/unban/{targetUserId}")
    @Operation(
            summary = "채팅방 사용자 강퇴 해제",
            description = "방장이 강퇴된 사용자의 강퇴를 해제합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> unbanUser(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "채팅방 ID", required = true)
            @PathVariable Long chatroomId,
            @Parameter(description = "강퇴 해제할 사용자 ID", required = true)
            @PathVariable Long targetUserId) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("사용자 강퇴 해제 요청 - 방장: {}, 채팅방: {}, 대상: {}", userId, chatroomId, targetUserId);

        chatService.unbanUser(chatroomId, userId, targetUserId);

        log.info("사용자 강퇴 해제 완료 - 대상: {}, 채팅방: {}", targetUserId, chatroomId);
        return ResponseEntity.ok(ApiResponse.success("사용자의 강퇴가 해제되었습니다."));
    }

    @GetMapping("/search")
    @Operation(
            summary = "채팅방 검색",
            description = "채팅방 이름과 참여자 이름으로 검색합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> searchChatRooms(
            @RequestHeader("Authorization") String authHeader,
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam String keyword) {

        Long userId = extractUserIdFromToken(authHeader);
        log.debug("채팅방 검색 요청 - 사용자: {}, 키워드: {}", userId, keyword);

        List<ChatRoomResponseDto> chatRooms = chatService.searchChatRooms(keyword, userId);

        log.debug("채팅방 검색 완료 - 사용자: {}, 결과 수: {}", userId, chatRooms.size());
        return ResponseEntity.ok(ApiResponse.success("채팅방 검색이 완료되었습니다.", chatRooms));
    }

    private Long extractUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7); // "Bearer " 제거
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}