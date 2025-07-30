package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.RandomChatStartRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.RandomChatMatchResponseDto;
import com.sonsminpark.auratalkback.domain.chat.service.RandomChatService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/random-chat")
@RequiredArgsConstructor
@Tag(name = "RandomChat", description = "랜덤 채팅 관련 API")
public class RandomChatController {

    private final RandomChatService randomChatService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/start")
    @Operation(
            summary = "랜덤 채팅 시작",
            description = "관심사 기반으로 랜덤 채팅을 시작합니다. 같은 관심사를 가진 사용자와 즉시 매칭하여 채팅방을 생성합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<RandomChatMatchResponseDto>> startRandomChat(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody RandomChatStartRequestDto requestDto) {

        Long userId = extractUserIdFromToken(authHeader);
        log.info("랜덤 채팅 시작 요청 - 사용자: {}", userId);

        RandomChatMatchResponseDto responseDto = randomChatService.startRandomChat(userId, requestDto);

        if (responseDto.isMatched()) {
            log.info("랜덤 채팅 매칭 성공 - 사용자: {}, 채팅방: {}", userId, responseDto.getChatRoom().getId());
            return ResponseEntity.ok(ApiResponse.success("매칭이 완료되었습니다!", responseDto));
        } else {
            log.info("랜덤 채팅 매칭 실패 - 사용자: {}, 사유: {}", userId, responseDto.getMessage());
            return ResponseEntity.ok(ApiResponse.success(responseDto.getMessage(), responseDto));
        }
    }

    private Long extractUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}