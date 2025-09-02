package com.sonsminpark.auratalkback.domain.chat.controller;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatFileUploadRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileDownloadResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatFileType;
import com.sonsminpark.auratalkback.domain.chat.service.ChatFileService;
import com.sonsminpark.auratalkback.global.common.ApiResponse;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import com.sonsminpark.auratalkback.global.s3.S3Service;
import com.sonsminpark.auratalkback.global.s3.UploadType;
import com.sonsminpark.auratalkback.global.s3.dto.request.PresignedUploadRequestDto;
import com.sonsminpark.auratalkback.global.s3.dto.response.PresignedUploadResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Chat File", description = "채팅 파일 전송/다운로드 관련 API")
public class ChatFileController {

    private final ChatFileService chatFileService;
    private final S3Service s3Service;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/{chatroomId}/files/presigned-url")
    @Operation(
            summary = "채팅 파일 업로드용 S3 PresignedUrl 생성",
            description = "채팅방에 파일 업로드를 위한 S3 PresignedUrl을 생성합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<PresignedUploadResponseDto>> getFileUploadUrl(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @Valid @RequestBody PresignedUploadRequestDto presignedUploadRequestDto) {

        Long userId = extractUserIdFromToken(authHeader);

        chatFileService.validateChatRoomAccess(chatroomId, userId);

        PresignedUploadResponseDto responseDto =
                s3Service.generatePresignedUploadUrl(UploadType.CHAT, presignedUploadRequestDto);

        return ResponseEntity.ok(ApiResponse.success("파일 업로드 URL이 생성되었습니다.", responseDto));
    }

    @PostMapping("/upload/{chatroomId}")
    @Operation(
            summary = "파일 업로드 완료 처리",
            description = "S3에 파일 업로드가 완료된 후 호출하여 채팅 메시지로 전송합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatFileResponseDto>> uploadFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @Valid @RequestBody ChatFileUploadRequestDto requestDto) {

        Long userId = extractUserIdFromToken(authHeader);
        ChatFileResponseDto responseDto = chatFileService.completeFileUpload(chatroomId, userId, requestDto);

        return ResponseEntity.ok(ApiResponse.success("파일이 성공적으로 업로드되었습니다.", responseDto));
    }

    @GetMapping("/download/{fileId}")
    @Operation(
            summary = "파일 다운로드",
            description = "채팅방의 파일을 다운로드합니다. 다운로드 URL을 반환합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<ChatFileDownloadResponseDto>> downloadFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long fileId) {

        Long userId = extractUserIdFromToken(authHeader);
        ChatFileDownloadResponseDto responseDto = chatFileService.generateDownloadUrl(fileId, userId);

        return ResponseEntity.ok(ApiResponse.success("파일 다운로드 URL이 생성되었습니다.", responseDto));
    }

    @GetMapping("/{chatroomId}/files")
    @Operation(
            summary = "채팅방 파일 목록 조회",
            description = "채팅방에 업로드된 파일 목록을 조회합니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Page<ChatFileResponseDto>>> getChatRoomFiles(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = extractUserIdFromToken(authHeader);
        Page<ChatFileResponseDto> files = chatFileService.getChatRoomFiles(chatroomId, userId, page, size);

        return ResponseEntity.ok(ApiResponse.success("파일 목록을 성공적으로 조회했습니다.", files));
    }

    @GetMapping("/{chatroomId}/files/by-type")
    @Operation(
            summary = "채팅방 파일 타입별 조회",
            description = "채팅방에 업로드된 파일을 타입별로 분류하여 조회합니다. (IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER)",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Page<ChatFileResponseDto>>> getChatRoomFilesByType(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long chatroomId,
            @Parameter(description = "파일 타입", required = true,
                    example = "IMAGE",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"IMAGE", "VIDEO", "AUDIO", "DOCUMENT", "OTHER"}))
            @RequestParam String fileType,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = extractUserIdFromToken(authHeader);

        try {
            ChatFileType type = ChatFileType.valueOf(fileType.toUpperCase());
            Page<ChatFileResponseDto> files = chatFileService.getChatRoomFilesByType(chatroomId, userId, type, page, size);

            return ResponseEntity.ok(ApiResponse.success(
                    type.getDescription() + " 파일 목록을 성공적으로 조회했습니다.", files));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE,
                            "올바르지 않은 파일 타입입니다. 사용 가능한 타입: IMAGE, VIDEO, AUDIO, DOCUMENT, OTHER"));
        }
    }

    @DeleteMapping("/files/{fileId}")
    @Operation(
            summary = "채팅 파일 삭제",
            description = "업로드한 파일을 삭제합니다. 파일 업로드자만 삭제할 수 있습니다.",
            security = {@SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long fileId) {

        Long userId = extractUserIdFromToken(authHeader);
        chatFileService.deleteFile(fileId, userId);

        return ResponseEntity.ok(ApiResponse.success("파일이 성공적으로 삭제되었습니다."));
    }

    private Long extractUserIdFromToken(String authHeader) {
        String token = authHeader.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}