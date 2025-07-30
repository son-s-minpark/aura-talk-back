package com.sonsminpark.auratalkback.domain.friend.controller;

import com.sonsminpark.auratalkback.domain.friend.dto.response.FriendListResponseDto;
import com.sonsminpark.auratalkback.domain.friend.dto.response.FriendRequestResponseDto;
import com.sonsminpark.auratalkback.domain.friend.service.FriendService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/friends")
@RestController
@Tag(name = "Friend", description = "친구 관련 API")
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/requests/{userId}")
    @Operation(
            summary = "친구 요청 보내기",
            description = "다른 사용자에게 친구 요청을 보냅니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<FriendRequestResponseDto>> sendFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "친구 요청을 받을 사용자 ID") @PathVariable Long userId) {
        FriendRequestResponseDto responseDto = friendService.sendFriendRequest(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 정상적으로 보냈습니다.", responseDto));
    }

    @PutMapping("/requests/{userId}/accept")
    @Operation(
            summary = "친구 요청 수락하기",
            description = "받은 친구 요청을 수락합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> acceptFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "친구 요청을 보낸 사용자 ID") @PathVariable Long userId) {
        friendService.acceptFriendRequest(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 정상적으로 수락했습니다."));
    }

    @DeleteMapping("/requests/{userId}/cancel")
    @Operation(
            summary = "친구 요청 취소하기",
            description = "내가 보낸 친구 요청을 취소합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> cancelFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "친구 요청을 취소할 대상 사용자 ID") @PathVariable Long userId) {
        friendService.removeFriendRequest(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 정상적으로 취소했습니다."));
    }

    @DeleteMapping("/requests/{userId}/reject")
    @Operation(
            summary = "친구 요청 거절하기",
            description = "받은 친구 요청을 거절합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> rejectFriendRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "친구 요청을 거절할 대상 사용자 ID") @PathVariable Long userId) {
        friendService.removeFriendRequest(userId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("친구 요청을 정상적으로 거절했습니다."));
    }

    @GetMapping("/requests/sent")
    @Operation(
            summary = "보낸 친구 요청 목록 조회",
            description = "내가 보낸 친구 요청 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<FriendListResponseDto>>> getSentFriendRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<FriendListResponseDto> sentFriendRequests = friendService.getSentFriendRequests(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("내가 보낸 친구 요청을 정상적으로 조회했습니다.", sentFriendRequests));
    }

    @GetMapping("/requests/received")
    @Operation(
            summary = "받은 친구 요청 목록 조회",
            description = "내가 받은 친구 요청 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<FriendListResponseDto>>> getReceivedFriendRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<FriendListResponseDto> receivedFriendRequests = friendService.getReceivedFriendRequests(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("내가 받은 친구 요청을 정상적으로 조회했습니다.", receivedFriendRequests));
    }

    @GetMapping
    @Operation(
            summary = "친구 목록 조회",
            description = "내 친구 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<FriendListResponseDto>>> getAllFriends(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<FriendListResponseDto> friends = friendService.getFriends(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("친구 목록을 정상적으로 조회했습니다.", friends));
    }

    @PostMapping("/blocks/{userId}")
    @Operation(
            summary = "사용자 차단하기",
            description = "특정 사용자를 차단합니다. 차단 시 기존 친구 관계는 해제됩니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> blockFriend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "차단할 사용자 ID") @PathVariable Long userId) {
        friendService.blockFriend(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success("사용자를 정상적으로 차단했습니다."));
    }

    @DeleteMapping("/blocks/{userId}")
    @Operation(
            summary = "사용자 차단 해제하기",
            description = "차단된 사용자의 차단을 해제합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> unblockFriend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "차단 해제할 사용자 ID") @PathVariable Long userId) {
        friendService.unblockFriend(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success("사용자를 정상적으로 차단해제했습니다."));
    }

    @GetMapping("/blocks")
    @Operation(
            summary = "차단한 사용자 목록 조회",
            description = "내가 차단한 사용자 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<FriendListResponseDto>>> getBlockedFriends(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<FriendListResponseDto> blockedFriends = friendService.getBlockedFriends(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("차단한 친구 목록을 조회했습니다.", blockedFriends));
    }

    @DeleteMapping("/{userId}")
    @Operation(
            summary = "친구 삭제하기",
            description = "친구 관계를 삭제합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> deleteFriend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "친구 관계를 삭제할 사용자 ID") @PathVariable Long userId) {
        friendService.deleteFriend(userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success("친구를 정상적으로 삭제했습니다."));
    }

    @GetMapping("/search")
    @Operation(
            summary = "친구 목록 검색",
            description = "친구 목록에서 사용자명 또는 닉네임으로 친구를 검색합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<FriendListResponseDto>>> searchFriends(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "검색 키워드 (사용자명 또는 닉네임)", required = true)
            @RequestParam String keyword) {
        List<FriendListResponseDto> friends = friendService.searchFriends(userDetails.getUserId(), keyword);
        return ResponseEntity.ok(ApiResponse.success("친구 검색이 완료되었습니다.", friends));
    }

    @GetMapping("/search/users")
    @Operation(
            summary = "사용자 검색 (친구 추가용)",
            description = "친구 추가를 위해 전체 사용자를 사용자명 또는 닉네임으로 검색합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<List<FriendListResponseDto>>> searchUsers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "검색 키워드 (사용자명 또는 닉네임)", required = true)
            @RequestParam String keyword) {
        List<FriendListResponseDto> users = friendService.searchUsers(userDetails.getUserId(), keyword);
        return ResponseEntity.ok(ApiResponse.success("사용자 검색이 완료되었습니다.", users));
    }
}