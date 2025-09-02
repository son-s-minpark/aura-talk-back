package com.sonsminpark.auratalkback.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatFileUploadRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileDownloadResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatUserResponseDto;
import com.sonsminpark.auratalkback.domain.chat.exception.*;
import com.sonsminpark.auratalkback.domain.chat.service.ChatFileService;
import com.sonsminpark.auratalkback.global.exception.GlobalExceptionHandler;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import com.sonsminpark.auratalkback.global.s3.S3Service;
import com.sonsminpark.auratalkback.global.s3.dto.request.PresignedUploadRequestDto;
import com.sonsminpark.auratalkback.global.s3.dto.response.PresignedUploadResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatFileController 테스트")
class ChatFileControllerTest {

    @Mock
    private ChatFileService chatFileService;

    @Mock
    private S3Service s3Service;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private ChatFileController chatFileController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ChatFileResponseDto testFileResponse;
    private ChatFileDownloadResponseDto testDownloadResponse;
    private ChatUserResponseDto testUploaderResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(chatFileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(post("/").characterEncoding(StandardCharsets.UTF_8))
                .alwaysDo(print())
                .build();

        testUploaderResponse = ChatUserResponseDto.builder()
                .id(1L)
                .nickname("테스트업로더")
                .thumbnailImageUrl("http://test.com/uploader_thumb.png")
                .build();

        testFileResponse = ChatFileResponseDto.builder()
                .fileId(1L)
                .chatroomId(1L)
                .messageId(1L)
                .originalFileName("test-file.jpg")
                .fileExtension("jpg")
                .mimeType("image/jpeg")
                .fileSize(1024L)
                .formattedFileSize("1.0 KB")
                .isImage(true)
                .isVideo(false)
                .isAudio(false)
                .uploader(testUploaderResponse)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        testDownloadResponse = ChatFileDownloadResponseDto.builder()
                .fileId(1L)
                .originalFileName("test-file.jpg")
                .downloadUrl("https://test-bucket.s3.amazonaws.com/presigned-download-url")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .fileSize(1024L)
                .formattedFileSize("1.0 KB")
                .mimeType("image/jpeg")
                .build();
    }

    @Nested
    @DisplayName("파일 업로드용 S3 PresignedUrl 생성 테스트")
    class GetFileUploadUrlTest {

        @Test
        @DisplayName("성공: 파일 업로드 URL 생성")
        void getFileUploadUrl_Success() throws Exception {
            // Given
            PresignedUploadRequestDto requestDto = new PresignedUploadRequestDto("test-file.jpg");
            PresignedUploadResponseDto responseDto = new PresignedUploadResponseDto(
                    "https://test-bucket.s3.amazonaws.com/presigned-upload-url",
                    "chat-files/test-file.jpg"
            );

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatFileService).validateChatRoomAccess(1L, 1L);
            given(s3Service.generatePresignedUploadUrl(any(), any())).willReturn(responseDto);

            // When + Then
            mockMvc.perform(post("/api/chats/1/files/presigned-url")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("파일 업로드 URL이 생성되었습니다."))
                    .andExpect(jsonPath("$.data.url").value(responseDto.getUrl()))
                    .andExpect(jsonPath("$.data.s3Key").value(responseDto.getS3Key()));

            verify(jwtTokenProvider).getUserIdFromToken("test-token");
            verify(chatFileService).validateChatRoomAccess(1L, 1L);
            verify(s3Service).generatePresignedUploadUrl(any(), any());
        }

        @Test
        @DisplayName("실패: 채팅방 접근 권한 없음")
        void getFileUploadUrl_AccessDenied() throws Exception {
            // Given
            PresignedUploadRequestDto requestDto = new PresignedUploadRequestDto("test-file.jpg");

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willThrow(ChatAccessDeniedException.of("채팅방에 접근할 권한이 없습니다."))
                    .given(chatFileService).validateChatRoomAccess(1L, 1L);

            // When + Then
            mockMvc.perform(post("/api/chats/1/files/presigned-url")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }

        @Test
        @DisplayName("성공: 빈 파일명도 허용됨")
        void getFileUploadUrl_EmptyFileName() throws Exception {
            // Given
            String requestJson = """
                {
                    "fileName": ""
                }
                """;

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatFileService).validateChatRoomAccess(1L, 1L);
            given(s3Service.generatePresignedUploadUrl(any(), any())).willReturn(null);

            // When + Then
            mockMvc.perform(post("/api/chats/1/files/presigned-url")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방")
        void getFileUploadUrl_ChatRoomNotFound() throws Exception {
            // Given
            PresignedUploadRequestDto requestDto = new PresignedUploadRequestDto("test-file.jpg");

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willThrow(ChatRoomNotFoundException.of(999L))
                    .given(chatFileService).validateChatRoomAccess(999L, 1L);

            // When + Then
            mockMvc.perform(post("/api/chats/999/files/presigned-url")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(440));
        }
    }

    @Nested
    @DisplayName("파일 업로드 완료 처리 테스트")
    class UploadFileTest {

        @Test
        @DisplayName("성공: 파일 업로드 완료")
        void uploadFile_Success() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/test-file.jpg")
                    .originalFileName("test-file.jpg")
                    .fileSize(1024L)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(testFileResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("파일이 성공적으로 업로드되었습니다."))
                    .andExpect(jsonPath("$.data.fileId").value(1L))
                    .andExpect(jsonPath("$.data.originalFileName").value("test-file.jpg"))
                    .andExpect(jsonPath("$.data.fileExtension").value("jpg"))
                    .andExpect(jsonPath("$.data.mimeType").value("image/jpeg"))
                    .andExpect(jsonPath("$.data.image").value(true))
                    .andExpect(jsonPath("$.data.uploader.nickname").value("테스트업로더"));

            verify(jwtTokenProvider).getUserIdFromToken("test-token");
            verify(chatFileService).completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class));
        }

        @Test
        @DisplayName("실패: 파일 크기 초과")
        void uploadFile_FileSizeExceeded() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/large-file.jpg")
                    .originalFileName("large-file.jpg")
                    .fileSize(200L * 1024 * 1024) // 200MB
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willThrow(ChatFileUploadException.fileSizeExceeded(100L * 1024 * 1024));

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(520));
        }

        @Test
        @DisplayName("실패: 필수 필드 누락")
        void uploadFile_MissingRequiredFields() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("") // 빈 S3 키
                    .originalFileName("") // 빈 파일명
                    .fileSize(null) // null 파일 크기
                    .build();

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Nested
    @DisplayName("파일 다운로드 테스트")
    class DownloadFileTest {

        @Test
        @DisplayName("성공: 파일 다운로드 URL 생성")
        void downloadFile_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.generateDownloadUrl(1L, 1L)).willReturn(testDownloadResponse);

            // When + Then
            mockMvc.perform(get("/api/chats/download/1")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("파일 다운로드 URL이 생성되었습니다."))
                    .andExpect(jsonPath("$.data.fileId").value(1L))
                    .andExpect(jsonPath("$.data.originalFileName").value("test-file.jpg"))
                    .andExpect(jsonPath("$.data.downloadUrl").value(testDownloadResponse.getDownloadUrl()))
                    .andExpect(jsonPath("$.data.fileSize").value(1024L))
                    .andExpect(jsonPath("$.data.mimeType").value("image/jpeg"));

            verify(jwtTokenProvider).getUserIdFromToken("test-token");
            verify(chatFileService).generateDownloadUrl(1L, 1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 파일")
        void downloadFile_FileNotFound() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.generateDownloadUrl(999L, 1L))
                    .willThrow(ChatFileNotFoundException.of(999L));

            // When + Then
            mockMvc.perform(get("/api/chats/download/999")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("실패: 파일 다운로드 권한 없음")
        void downloadFile_AccessDenied() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            given(chatFileService.generateDownloadUrl(1L, 2L))
                    .willThrow(ChatAccessDeniedException.of("파일에 접근할 권한이 없습니다."));

            // When + Then
            mockMvc.perform(get("/api/chats/download/1")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }

        @Test
        @DisplayName("실패: 잘못된 파일 ID 형식")
        void downloadFile_InvalidFileId() throws Exception {
            // When + Then
            mockMvc.perform(get("/api/chats/download/invalid")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(402)); // INVALID_TYPE_VALUE
        }
    }

    @Nested
    @DisplayName("채팅방 파일 목록 조회 테스트")
    class GetChatRoomFilesTest {

        @Test
        @DisplayName("성공: 파일 목록 조회")
        void getChatRoomFiles_Success() throws Exception {
            // Given
            List<ChatFileResponseDto> files = List.of(testFileResponse);
            Page<ChatFileResponseDto> filePage = new PageImpl<>(files, PageRequest.of(0, 20), 1);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.getChatRoomFiles(1L, 1L, 0, 20)).willReturn(filePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1/files")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "0")
                            .param("size", "20")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("파일 목록을 성공적으로 조회했습니다."))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].fileId").value(1L))
                    .andExpect(jsonPath("$.data.content[0].originalFileName").value("test-file.jpg"))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.number").value(0));

            verify(jwtTokenProvider).getUserIdFromToken("test-token");
            verify(chatFileService).getChatRoomFiles(1L, 1L, 0, 20);
        }

        @Test
        @DisplayName("성공: 기본 페이징 파라미터")
        void getChatRoomFiles_DefaultPaging() throws Exception {
            // Given
            Page<ChatFileResponseDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.getChatRoomFiles(1L, 1L, 0, 20)).willReturn(emptyPage);

            // When + Then
            mockMvc.perform(get("/api/chats/1/files")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.number").value(0));
        }

        @Test
        @DisplayName("성공: 커스텀 페이징 파라미터")
        void getChatRoomFiles_CustomPaging() throws Exception {
            // Given
            Page<ChatFileResponseDto> filePage = new PageImpl<>(List.of(), PageRequest.of(1, 10), 0);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.getChatRoomFiles(1L, 1L, 1, 10)).willReturn(filePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1/files")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "1")
                            .param("size", "10")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.number").value(1));
        }

        @Test
        @DisplayName("실패: 채팅방 접근 권한 없음")
        void getChatRoomFiles_AccessDenied() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            given(chatFileService.getChatRoomFiles(1L, 2L, 0, 20))
                    .willThrow(ChatAccessDeniedException.of("채팅방에 접근할 권한이 없습니다."));

            // When + Then
            mockMvc.perform(get("/api/chats/1/files")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }
    }

    @Nested
    @DisplayName("파일 삭제 테스트")
    class DeleteFileTest {

        @Test
        @DisplayName("성공: 파일 삭제")
        void deleteFile_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatFileService).deleteFile(1L, 1L);

            // When + Then
            mockMvc.perform(delete("/api/chats/files/1")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("파일이 성공적으로 삭제되었습니다."))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(jwtTokenProvider).getUserIdFromToken("test-token");
            verify(chatFileService).deleteFile(1L, 1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 파일")
        void deleteFile_FileNotFound() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willThrow(ChatFileNotFoundException.of(999L)).given(chatFileService).deleteFile(999L, 1L);

            // When + Then
            mockMvc.perform(delete("/api/chats/files/999")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("실패: 파일 삭제 권한 없음 (다른 사용자의 파일)")
        void deleteFile_NotOwner() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            willThrow(ChatFileNotFoundException.of("파일을 찾을 수 없거나 삭제 권한이 없습니다."))
                    .given(chatFileService).deleteFile(1L, 2L);

            // When + Then
            mockMvc.perform(delete("/api/chats/files/1")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("실패: 잘못된 파일 ID 형식")
        void deleteFile_InvalidFileId() throws Exception {
            // When + Then
            mockMvc.perform(delete("/api/chats/files/invalid")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(402)); // INVALID_TYPE_VALUE
        }
    }

    @Nested
    @DisplayName("Authorization 헤더 처리 테스트")
    class AuthorizationHeaderTest {

        @Test
        @DisplayName("성공: 유효한 JWT 토큰")
        void validJwtToken() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("valid-token")).willReturn(1L);
            given(chatFileService.generateDownloadUrl(1L, 1L)).willReturn(testDownloadResponse);

            // When + Then
            mockMvc.perform(get("/api/chats/download/1")
                            .header("Authorization", "Bearer valid-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(jwtTokenProvider).getUserIdFromToken("valid-token");
        }

        @Test
        @DisplayName("실패: Authorization 헤더 누락")
        void missingAuthorizationHeader() throws Exception {
            // When + Then
            mockMvc.perform(get("/api/chats/download/1")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 JWT 토큰")
        void invalidJwtToken() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("invalid-token"))
                    .willThrow(new RuntimeException("Invalid token"));

            // When + Then
            mockMvc.perform(get("/api/chats/download/1")
                            .header("Authorization", "Bearer invalid-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("응답 형식 검증 테스트")
    class ResponseFormatTest {

        @Test
        @DisplayName("성공: 표준 API 응답 형식")
        void standardApiResponseFormat() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.generateDownloadUrl(1L, 1L)).willReturn(testDownloadResponse);

            // When + Then
            mockMvc.perform(get("/api/chats/download/1")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.success").isBoolean())
                    .andExpect(jsonPath("$.message").isString());
        }

        @Test
        @DisplayName("성공: 페이징 정보 포함")
        void pagingInformationIncluded() throws Exception {
            // Given
            List<ChatFileResponseDto> files = List.of(testFileResponse);
            Page<ChatFileResponseDto> filePage = new PageImpl<>(
                    files,
                    PageRequest.of(0, 20),
                    100
            );

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.getChatRoomFiles(1L, 1L, 0, 20)).willReturn(filePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1/files")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(100))
                    .andExpect(jsonPath("$.data.totalPages").value(5))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("파일 ID 경계값 - 최솟값")
        void downloadFile_MinFileId() throws Exception {
            // Given
            Long minFileId = 1L;
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.generateDownloadUrl(minFileId, 1L)).willReturn(testDownloadResponse);

            // When + Then
            mockMvc.perform(get("/api/chats/download/1")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(chatFileService).generateDownloadUrl(minFileId, 1L);
        }

        @Test
        @DisplayName("파일 ID 경계값 - 최댓값")
        void downloadFile_MaxFileId() throws Exception {
            // Given
            Long maxFileId = Long.MAX_VALUE;
            ChatFileDownloadResponseDto maxResponse = ChatFileDownloadResponseDto.builder()
                    .fileId(maxFileId)
                    .originalFileName("max-file.jpg")
                    .downloadUrl("https://test-bucket.s3.amazonaws.com/max-presigned-url")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .fileSize(1024L)
                    .formattedFileSize("1.0 KB")
                    .mimeType("image/jpeg")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.generateDownloadUrl(maxFileId, 1L)).willReturn(maxResponse);

            // When + Then
            mockMvc.perform(get("/api/chats/download/" + maxFileId)
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.fileId").value(maxFileId));
        }

        @Test
        @DisplayName("채팅방 ID 경계값 - 최솟값")
        void getChatRoomFiles_MinChatroomId() throws Exception {
            // Given
            Long minChatroomId = 1L;
            Page<ChatFileResponseDto> filePage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.getChatRoomFiles(minChatroomId, 1L, 0, 20)).willReturn(filePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1/files")
                            .header("Authorization", "Bearer test-token")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(chatFileService).getChatRoomFiles(minChatroomId, 1L, 0, 20);
        }

        @Test
        @DisplayName("페이지 크기 경계값 - 최솟값")
        void getChatRoomFiles_MinPageSize() throws Exception {
            // Given
            Page<ChatFileResponseDto> filePage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.getChatRoomFiles(1L, 1L, 0, 1)).willReturn(filePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1/files")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "0")
                            .param("size", "1")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.size").value(1));
        }

        @Test
        @DisplayName("페이지 크기 경계값 - 큰 값")
        void getChatRoomFiles_LargePageSize() throws Exception {
            // Given
            Page<ChatFileResponseDto> filePage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.getChatRoomFiles(1L, 1L, 0, 100)).willReturn(filePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1/files")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "0")
                            .param("size", "100")
                            .characterEncoding(StandardCharsets.UTF_8))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.size").value(100));
        }

        @Test
        @DisplayName("파일 크기 경계값 - 0바이트")
        void uploadFile_ZeroByteFile() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/empty-file.txt")
                    .originalFileName("empty-file.txt")
                    .fileSize(0L)
                    .build();

            ChatFileResponseDto zeroSizeResponse = ChatFileResponseDto.builder()
                    .fileId(1L)
                    .chatroomId(1L)
                    .messageId(1L)
                    .originalFileName("empty-file.txt")
                    .fileExtension("txt")
                    .mimeType("text/plain")
                    .fileSize(0L)
                    .formattedFileSize("0 B")
                    .isImage(false)
                    .isVideo(false)
                    .isAudio(false)
                    .uploader(testUploaderResponse)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(zeroSizeResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.fileSize").value(0L))
                    .andExpect(jsonPath("$.data.formattedFileSize").value("0 B"));
        }

        @Test
        @DisplayName("파일명 경계값 - 매우 긴 파일명")
        void uploadFile_VeryLongFileName() throws Exception {
            // Given
            String longFileName = "a".repeat(255) + ".jpg";
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/" + longFileName)
                    .originalFileName(longFileName)
                    .fileSize(1024L)
                    .build();

            ChatFileResponseDto longNameResponse = ChatFileResponseDto.builder()
                    .fileId(1L)
                    .chatroomId(1L)
                    .messageId(1L)
                    .originalFileName(longFileName)
                    .fileExtension("jpg")
                    .mimeType("image/jpeg")
                    .fileSize(1024L)
                    .formattedFileSize("1.0 KB")
                    .isImage(true)
                    .isVideo(false)
                    .isAudio(false)
                    .uploader(testUploaderResponse)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(longNameResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.originalFileName").value(longFileName));
        }

        @Test
        @DisplayName("특수 문자가 포함된 파일명")
        void uploadFile_SpecialCharacterFileName() throws Exception {
            // Given
            String specialFileName = "파일이름_with-특수문자@#$%^&().jpg";
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/encoded-file-name.jpg")
                    .originalFileName(specialFileName)
                    .fileSize(1024L)
                    .build();

            ChatFileResponseDto specialNameResponse = ChatFileResponseDto.builder()
                    .fileId(1L)
                    .chatroomId(1L)
                    .messageId(1L)
                    .originalFileName(specialFileName)
                    .fileExtension("jpg")
                    .mimeType("image/jpeg")
                    .fileSize(1024L)
                    .formattedFileSize("1.0 KB")
                    .isImage(true)
                    .isVideo(false)
                    .isAudio(false)
                    .uploader(testUploaderResponse)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(specialNameResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.originalFileName").value(specialFileName));
        }

        @Test
        @DisplayName("서비스에서 예외 발생 시 예외 처리 흐름")
        void uploadFile_ServiceException_HandledCorrectly() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/test-file.jpg")
                    .originalFileName("test-file.jpg")
                    .fileSize(1024L)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willThrow(new RuntimeException("서비스 에러"));

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("응답에서 필수 필드들이 제대로 설정되는지 확인")
        void uploadFile_ResponseFieldsValidation() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/test-file.jpg")
                    .originalFileName("test-file.jpg")
                    .fileSize(1024L)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(testFileResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.fileId").exists())
                    .andExpect(jsonPath("$.data.chatroomId").exists())
                    .andExpect(jsonPath("$.data.originalFileName").exists())
                    .andExpect(jsonPath("$.data.fileExtension").exists())
                    .andExpect(jsonPath("$.data.mimeType").exists())
                    .andExpect(jsonPath("$.data.fileSize").exists())
                    .andExpect(jsonPath("$.data.uploader").exists())
                    .andExpect(jsonPath("$.data.createdAt").exists())
                    .andExpect(jsonPath("$.data.deleted").exists());
        }
    }

    @Nested
    @DisplayName("다양한 파일 타입 테스트")
    class FileTypeTest {

        @Test
        @DisplayName("성공: 이미지 파일 업로드")
        void uploadFile_ImageFile() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/image.png")
                    .originalFileName("image.png")
                    .fileSize(2048L)
                    .build();

            ChatFileResponseDto imageResponse = ChatFileResponseDto.builder()
                    .fileId(1L)
                    .chatroomId(1L)
                    .messageId(1L)
                    .originalFileName("image.png")
                    .fileExtension("png")
                    .mimeType("image/png")
                    .fileSize(2048L)
                    .formattedFileSize("2.0 KB")
                    .isImage(true)
                    .isVideo(false)
                    .isAudio(false)
                    .uploader(testUploaderResponse)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(imageResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.image").value(true))
                    .andExpect(jsonPath("$.data.video").value(false))
                    .andExpect(jsonPath("$.data.audio").value(false));
        }

        @Test
        @DisplayName("성공: 비디오 파일 업로드")
        void uploadFile_VideoFile() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/video.mp4")
                    .originalFileName("video.mp4")
                    .fileSize(10485760L) // 10MB
                    .build();

            ChatFileResponseDto videoResponse = ChatFileResponseDto.builder()
                    .fileId(1L)
                    .chatroomId(1L)
                    .messageId(1L)
                    .originalFileName("video.mp4")
                    .fileExtension("mp4")
                    .mimeType("video/mp4")
                    .fileSize(10485760L)
                    .formattedFileSize("10.0 MB")
                    .isImage(false)
                    .isVideo(true)
                    .isAudio(false)
                    .uploader(testUploaderResponse)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(videoResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.image").value(false))
                    .andExpect(jsonPath("$.data.video").value(true))
                    .andExpect(jsonPath("$.data.audio").value(false));
        }

        @Test
        @DisplayName("성공: 오디오 파일 업로드")
        void uploadFile_AudioFile() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/audio.mp3")
                    .originalFileName("audio.mp3")
                    .fileSize(5242880L) // 5MB
                    .build();

            ChatFileResponseDto audioResponse = ChatFileResponseDto.builder()
                    .fileId(1L)
                    .chatroomId(1L)
                    .messageId(1L)
                    .originalFileName("audio.mp3")
                    .fileExtension("mp3")
                    .mimeType("audio/mpeg")
                    .fileSize(5242880L)
                    .formattedFileSize("5.0 MB")
                    .isImage(false)
                    .isVideo(false)
                    .isAudio(true)
                    .uploader(testUploaderResponse)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(audioResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.image").value(false))
                    .andExpect(jsonPath("$.data.video").value(false))
                    .andExpect(jsonPath("$.data.audio").value(true));
        }

        @Test
        @DisplayName("성공: 문서 파일 업로드")
        void uploadFile_DocumentFile() throws Exception {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/document.pdf")
                    .originalFileName("document.pdf")
                    .fileSize(1048576L) // 1MB
                    .build();

            ChatFileResponseDto documentResponse = ChatFileResponseDto.builder()
                    .fileId(1L)
                    .chatroomId(1L)
                    .messageId(1L)
                    .originalFileName("document.pdf")
                    .fileExtension("pdf")
                    .mimeType("application/pdf")
                    .fileSize(1048576L)
                    .formattedFileSize("1.0 MB")
                    .isImage(false)
                    .isVideo(false)
                    .isAudio(false)
                    .uploader(testUploaderResponse)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatFileService.completeFileUpload(eq(1L), eq(1L), any(ChatFileUploadRequestDto.class)))
                    .willReturn(documentResponse);

            // When + Then
            mockMvc.perform(post("/api/chats/upload/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding(StandardCharsets.UTF_8)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data.image").value(false))
                    .andExpect(jsonPath("$.data.video").value(false))
                    .andExpect(jsonPath("$.data.audio").value(false))
                    .andExpect(jsonPath("$.data.mimeType").value("application/pdf"));
        }
    }
}