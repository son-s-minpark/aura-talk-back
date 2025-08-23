package com.sonsminpark.auratalkback.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomUpdateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.OneToOneChatRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.*;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import com.sonsminpark.auratalkback.domain.chat.exception.*;
import com.sonsminpark.auratalkback.domain.chat.service.ChatRoomImageService;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.exception.GlobalExceptionHandler;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import com.sonsminpark.auratalkback.global.s3.S3Service;
import com.sonsminpark.auratalkback.global.s3.UploadType;
import com.sonsminpark.auratalkback.global.s3.dto.request.PresignedUploadRequestDto;
import com.sonsminpark.auratalkback.global.s3.dto.request.UploadCompletedRequestDto;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatRoomController 테스트")
class ChatRoomControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ChatRoomImageService chatRoomImageService;

    @Mock
    private S3Service s3Service;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private ChatRoomController chatRoomController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ChatRoomResponseDto testChatRoomResponse;
    private ChatUserResponseDto testOwnerResponse;
    private ChatInviteResponseDto testInviteResponse;
    private PresignedUploadResponseDto testPresignedResponse;
    private ChatRoomImageResponseDto testImageResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(chatRoomController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        setupTestData();
    }

    private void setupTestData() {
        testOwnerResponse = ChatUserResponseDto.builder()
                .id(1L)
                .nickname("방장")
                .thumbnailImageUrl("http://test.com/owner_thumb.png")
                .build();

        ChatUserResponseDto memberResponse = ChatUserResponseDto.builder()
                .id(2L)
                .nickname("멤버")
                .thumbnailImageUrl("http://test.com/member_thumb.png")
                .build();

        testChatRoomResponse = ChatRoomResponseDto.builder()
                .id(1L)
                .name("테스트 채팅방")
                .type(ChatRoomType.GROUP)
                .owner(testOwnerResponse)
                .users(Arrays.asList(testOwnerResponse, memberResponse))
                .createdAt(LocalDateTime.now())
                .lastMessageAt(LocalDateTime.now())
                .roomImageUrl("http://test.com/room.png")
                .roomThumbnailImageUrl("http://test.com/room_thumb.png")
                .inviteCode("test-invite-code")
                .inviteCodeExpiredAt(LocalDateTime.now().plusHours(24))
                .build();

        testInviteResponse = ChatInviteResponseDto.builder()
                .inviteCode("test-invite-code")
                .inviteLink("https://auratalk.com/invite/test-invite-code")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        // 실제 DTO 구조에 맞게 생성
        testPresignedResponse = new PresignedUploadResponseDto(
                "https://test-bucket.s3.amazonaws.com/presigned-upload-url",
                "group-images/original/test.jpg"
        );

        testImageResponse = ChatRoomImageResponseDto.builder()
                .originalImageUrl("http://test.com/room.png")
                .thumbnailImageUrl("http://test.com/room_thumb.png")
                .build();
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    class CreateChatRoomTest {

        @Test
        @DisplayName("성공: 그룹 채팅방 생성")
        void createChatRoom_Success() throws Exception {
            // Given
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name("새로운 채팅방")
                    .userIds(Arrays.asList(2L, 3L))
                    .roomImageUrl("http://test.com/custom.png")
                    .roomThumbnailImageUrl("http://test.com/custom_thumb.png")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L))).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방이 성공적으로 생성되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.name").value("테스트 채팅방"))
                    .andExpect(jsonPath("$.data.type").value("GROUP"))
                    .andExpect(jsonPath("$.data.users").isArray())
                    .andExpect(jsonPath("$.data.users").isNotEmpty());

            verify(chatService).createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L));
        }

        @Test
        @DisplayName("성공: 기본 이미지로 채팅방 생성")
        void createChatRoom_WithDefaultImage() throws Exception {
            // Given
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name("기본 이미지 채팅방")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L))).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.roomImageUrl").exists())
                    .andExpect(jsonPath("$.data.roomThumbnailImageUrl").exists());
        }

        @Test
        @DisplayName("실패: 채팅방 이름 누락")
        void createChatRoom_MissingName() throws Exception {
            // Given - JSON으로 직접 작성 (name 필드 누락)
            String requestJson = "{\"userIds\":[2]}";

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("실패: 채팅방 이름 길이 초과")
        void createChatRoom_NameTooLong() throws Exception {
            // Given - JSON으로 직접 작성 (51자 이름)
            String longName = "a".repeat(51);
            String requestJson = "{\"name\":\"" + longName + "\"}";

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Nested
    @DisplayName("1:1 채팅방 생성 테스트")
    class CreateOneToOneChatRoomTest {

        @Test
        @DisplayName("성공: 1:1 채팅방 생성")
        void createOneToOneChatRoom_Success() throws Exception {
            // Given
            OneToOneChatRequestDto requestDto = OneToOneChatRequestDto.builder()
                    .targetUserId(2L)
                    .build();

            ChatRoomResponseDto oneToOneResponse = ChatRoomResponseDto.builder()
                    .id(2L)
                    .name("방장, 멤버")
                    .type(ChatRoomType.ONE_TO_ONE)
                    .owner(testOwnerResponse)
                    .users(Arrays.asList(testOwnerResponse, ChatUserResponseDto.builder().id(2L).nickname("멤버").build()))
                    .createdAt(LocalDateTime.now())
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createOneToOneChatRoom(1L, 2L)).willReturn(oneToOneResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms/one-to-one")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("1:1 채팅방이 준비되었습니다."))
                    .andExpect(jsonPath("$.data.id").value(2L))
                    .andExpect(jsonPath("$.data.type").value("ONE_TO_ONE"))
                    .andExpect(jsonPath("$.data.name").value("방장, 멤버"));

            verify(chatService).createOneToOneChatRoom(1L, 2L);
        }

        @Test
        @DisplayName("실패: 대상 사용자 ID 누락")
        void createOneToOneChatRoom_MissingTargetUserId() throws Exception {
            // Given - JSON으로 직접 작성 (targetUserId 누락)
            String requestJson = "{}";

            // When + Then
            mockMvc.perform(post("/api/chatrooms/one-to-one")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(400));
        }

        @Test
        @DisplayName("실패: 자기 자신과의 채팅방 생성")
        void createOneToOneChatRoom_SelfChat() throws Exception {
            // Given
            OneToOneChatRequestDto requestDto = OneToOneChatRequestDto.builder()
                    .targetUserId(1L)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createOneToOneChatRoom(1L, 1L))
                    .willThrow(new IllegalArgumentException("자기 자신과는 채팅할 수 없습니다."));

            // When + Then
            mockMvc.perform(post("/api/chatrooms/one-to-one")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    @Nested
    @DisplayName("채팅방 이미지 업로드 테스트")
    class RoomImageUploadTest {

        @Test
        @DisplayName("성공: 채팅방 이미지 업로드 URL 생성")
        void getRoomImageUploadUrl_Success() throws Exception {
            // Given
            String requestJson = "{\"fileName\":\"room.jpg\"}";

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(s3Service.generatePresignedUploadUrl(eq(UploadType.GROUP), any(PresignedUploadRequestDto.class)))
                    .willReturn(testPresignedResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms/room-image/presigned-url")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방 이미지 업로드 URL이 생성되었습니다."))
                    .andExpect(jsonPath("$.data.url").value(testPresignedResponse.getUrl()))
                    .andExpect(jsonPath("$.data.s3Key").value(testPresignedResponse.getS3Key()));

            verify(s3Service).generatePresignedUploadUrl(eq(UploadType.GROUP), any(PresignedUploadRequestDto.class));
        }

        @Test
        @DisplayName("성공: 채팅방 이미지 업로드 완료 처리")
        void saveRoomImageUrl_Success() throws Exception {
            // Given
            String requestJson = "{\"s3Key\":\"group-images/original/test.jpg\"}";

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatRoomImageService.processUploadedImage("group-images/original/test.jpg"))
                    .willReturn(testImageResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms/room-image/upload-complete")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방 이미지가 성공적으로 처리되었습니다."))
                    .andExpect(jsonPath("$.data.originalImageUrl").value(testImageResponse.getOriginalImageUrl()))
                    .andExpect(jsonPath("$.data.thumbnailImageUrl").value(testImageResponse.getThumbnailImageUrl()));

            verify(chatRoomImageService).processUploadedImage("group-images/original/test.jpg");
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회 테스트")
    class GetChatRoomsTest {

        @Test
        @DisplayName("성공: 채팅방 목록 조회")
        void getChatRooms_Success() throws Exception {
            // Given
            List<ChatRoomResponseDto> chatRooms = Arrays.asList(testChatRoomResponse);
            Page<ChatRoomResponseDto> chatRoomPage = new PageImpl<>(chatRooms, PageRequest.of(0, 20), 1);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomsByUserId(1L, 0, 20)).willReturn(chatRoomPage);

            // When + Then
            mockMvc.perform(get("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방 목록을 성공적으로 조회했습니다."))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].id").value(1L))
                    .andExpect(jsonPath("$.data.content[0].name").value("테스트 채팅방"))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.size").value(20));

            verify(chatService).getChatRoomsByUserId(1L, 0, 20);
        }

        @Test
        @DisplayName("성공: 기본 페이징 파라미터")
        void getChatRooms_DefaultPaging() throws Exception {
            // Given
            Page<ChatRoomResponseDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomsByUserId(1L, 0, 20)).willReturn(emptyPage);

            // When + Then
            mockMvc.perform(get("/api/chatrooms")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.number").value(0));
        }
    }

    @Nested
    @DisplayName("채팅방 정보 조회 테스트")
    class GetChatRoomInfoTest {

        @Test
        @DisplayName("성공: 채팅방 정보 조회")
        void getChatRoomInfo_Success() throws Exception {
            // Given
            ChatRoomResponseDto responseWithTotalPages = ChatRoomResponseDto.builder()
                    .id(1L)
                    .name("테스트 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testOwnerResponse)
                    .users(Arrays.asList(testOwnerResponse))
                    .createdAt(LocalDateTime.now())
                    .totalPages(10)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomInfo(1L, 1L)).willReturn(responseWithTotalPages);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방 정보를 성공적으로 조회했습니다."))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.name").value("테스트 채팅방"))
                    .andExpect(jsonPath("$.data.totalPages").value(10));

            verify(chatService).getChatRoomInfo(1L, 1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방")
        void getChatRoomInfo_ChatRoomNotFound() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomInfo(999L, 1L))
                    .willThrow(ChatRoomNotFoundException.of(999L));

            // When + Then
            mockMvc.perform(get("/api/chatrooms/999")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(440));
        }

        @Test
        @DisplayName("실패: 채팅방 접근 권한 없음")
        void getChatRoomInfo_AccessDenied() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            given(chatService.getChatRoomInfo(1L, 2L))
                    .willThrow(ChatAccessDeniedException.of("채팅방에 접근할 권한이 없습니다."));

            // When + Then
            mockMvc.perform(get("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }
    }

    @Nested
    @DisplayName("채팅방 정보 수정 테스트")
    class UpdateChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방 이름 수정")
        void updateChatRoom_Success() throws Exception {
            // Given
            ChatRoomUpdateRequestDto requestDto = ChatRoomUpdateRequestDto.builder()
                    .name("수정된 채팅방")
                    .build();

            ChatRoomResponseDto updatedResponse = ChatRoomResponseDto.builder()
                    .id(1L)
                    .name("수정된 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testOwnerResponse)
                    .users(Arrays.asList(testOwnerResponse))
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.updateChatRoom(eq(1L), any(ChatRoomUpdateRequestDto.class), eq(1L))).willReturn(updatedResponse);

            // When + Then
            mockMvc.perform(put("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방 정보가 성공적으로 수정되었습니다."))
                    .andExpect(jsonPath("$.data.name").value("수정된 채팅방"));

            verify(chatService).updateChatRoom(eq(1L), any(ChatRoomUpdateRequestDto.class), eq(1L));
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 수정 시도")
        void updateChatRoom_NotOwner() throws Exception {
            // Given
            ChatRoomUpdateRequestDto requestDto = ChatRoomUpdateRequestDto.builder()
                    .name("수정된 채팅방")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            given(chatService.updateChatRoom(eq(1L), any(ChatRoomUpdateRequestDto.class), eq(2L)))
                    .willThrow(InvalidChatRoomStateException.ownerRequired());

            // When + Then
            mockMvc.perform(put("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }

        @Test
        @DisplayName("실패: 채팅방 이름 길이 초과")
        void updateChatRoom_NameTooLong() throws Exception {
            // Given - JSON으로 직접 작성 (51자 이름)
            String longName = "a".repeat(51);
            String requestJson = "{\"name\":\"" + longName + "\"}";

            // When + Then
            mockMvc.perform(put("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(400));
        }
    }

    @Nested
    @DisplayName("채팅방 나가기 테스트")
    class LeaveChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방 나가기")
        void leaveChatRoom_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            willDoNothing().given(chatService).leaveChatRoom(1L, 2L);

            // When + Then
            mockMvc.perform(delete("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방을 나갔습니다."))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(chatService).leaveChatRoom(1L, 2L);
        }

        @Test
        @DisplayName("실패: 채팅방 멤버가 아닌 사용자")
        void leaveChatRoom_NotMember() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(3L);
            willThrow(InvalidChatRoomStateException.notMember())
                    .given(chatService).leaveChatRoom(1L, 3L);

            // When + Then
            mockMvc.perform(delete("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }
    }

    @Nested
    @DisplayName("채팅방 이미지 삭제 테스트")
    class DeleteRoomImageTest {

        @Test
        @DisplayName("성공: 채팅방 이미지 삭제")
        void deleteRoomImage_Success() throws Exception {
            // Given
            ChatRoomResponseDto responseWithDefaultImage = ChatRoomResponseDto.builder()
                    .id(1L)
                    .name("테스트 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testOwnerResponse)
                    .users(Arrays.asList(testOwnerResponse))
                    .roomImageUrl("http://test.com/default.png")
                    .roomThumbnailImageUrl("http://test.com/default_thumb.png")
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.deleteRoomImage(1L, 1L)).willReturn(responseWithDefaultImage);

            // When + Then
            mockMvc.perform(delete("/api/chatrooms/1/room-image")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방 이미지가 성공적으로 삭제되었습니다."))
                    .andExpect(jsonPath("$.data.roomImageUrl").value("http://test.com/default.png"));

            verify(chatService).deleteRoomImage(1L, 1L);
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 삭제 시도")
        void deleteRoomImage_NotOwner() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            given(chatService.deleteRoomImage(1L, 2L))
                    .willThrow(InvalidChatRoomStateException.ownerRequired());

            // When + Then
            mockMvc.perform(delete("/api/chatrooms/1/room-image")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }
    }

    @Nested
    @DisplayName("초대 링크 관련 테스트")
    class InviteLinkTest {

        @Test
        @DisplayName("성공: 친구에게 초대 링크 전송")
        void sendInviteToFriend_Success() throws Exception {
            // Given
            ChatInviteRequestDto requestDto = ChatInviteRequestDto.builder()
                    .userId(2L)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.sendInviteToFriend(eq(1L), any(ChatInviteRequestDto.class), eq(1L))).willReturn(testInviteResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms/1/invite")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("친구에게 초대 링크를 전송했습니다."))
                    .andExpect(jsonPath("$.data.inviteCode").value("test-invite-code"))
                    .andExpect(jsonPath("$.data.inviteLink").value("https://auratalk.com/invite/test-invite-code"));

            verify(chatService).sendInviteToFriend(eq(1L), any(ChatInviteRequestDto.class), eq(1L));
        }

        @Test
        @DisplayName("성공: 초대 링크 생성")
        void createInviteLink_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createInviteLink(1L, 1L)).willReturn(testInviteResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms/1/invite-link")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("초대 링크가 생성되었습니다."))
                    .andExpect(jsonPath("$.data.inviteCode").exists())
                    .andExpect(jsonPath("$.data.inviteLink").exists())
                    .andExpect(jsonPath("$.data.expiresAt").exists());

            verify(chatService).createInviteLink(1L, 1L);
        }

        @Test
        @DisplayName("성공: 초대 링크로 채팅방 참여")
        void joinChatRoomByInviteLink_Success() throws Exception {
            // Given
            String inviteCode = "test-invite-code";

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            willDoNothing().given(chatService).acceptInvite(inviteCode, 2L);

            // When + Then
            mockMvc.perform(post("/api/chatrooms/join")
                            .header("Authorization", "Bearer test-token")
                            .param("inviteCode", inviteCode))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방에 참여했습니다."));

            verify(chatService).acceptInvite(inviteCode, 2L);
        }

        @Test
        @DisplayName("실패: 유효하지 않은 초대 코드")
        void joinChatRoomByInviteLink_InvalidCode() throws Exception {
            // Given
            String invalidCode = "invalid-code";

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            willThrow(ChatInvitationException.invalidInviteCode())
                    .given(chatService).acceptInvite(invalidCode, 2L);

            // When + Then
            mockMvc.perform(post("/api/chatrooms/join")
                            .header("Authorization", "Bearer test-token")
                            .param("inviteCode", invalidCode))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(404));
        }

        @Test
        @DisplayName("실패: 초대 코드 누락")
        void joinChatRoomByInviteLink_MissingCode() throws Exception {
            // When + Then
            mockMvc.perform(post("/api/chatrooms/join")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    @Nested
    @DisplayName("알림 설정 테스트")
    class NotificationSettingsTest {

        @Test
        @DisplayName("성공: 알림 활성화")
        void updateNotificationSettings_Enable() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatService).updateNotificationSettings(1L, 1L, true);

            // When + Then
            mockMvc.perform(put("/api/chatrooms/1/notification")
                            .header("Authorization", "Bearer test-token")
                            .param("enabled", "true"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 설정이 변경되었습니다."));

            verify(chatService).updateNotificationSettings(1L, 1L, true);
        }

        @Test
        @DisplayName("성공: 알림 비활성화")
        void updateNotificationSettings_Disable() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatService).updateNotificationSettings(1L, 1L, false);

            // When + Then
            mockMvc.perform(put("/api/chatrooms/1/notification")
                            .header("Authorization", "Bearer test-token")
                            .param("enabled", "false"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("알림 설정이 변경되었습니다."));

            verify(chatService).updateNotificationSettings(1L, 1L, false);
        }

        @Test
        @DisplayName("실패: enabled 파라미터 누락")
        void updateNotificationSettings_MissingParam() throws Exception {
            // When + Then
            mockMvc.perform(put("/api/chatrooms/1/notification")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));
        }
    }

    @Nested
    @DisplayName("채팅방 관리 테스트")
    class ChatRoomManagementTest {

        @Test
        @DisplayName("성공: 채팅방 삭제")
        void deleteChatRoom_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatService).deleteChatRoom(1L, 1L);

            // When + Then
            mockMvc.perform(delete("/api/chatrooms/1/delete")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방이 삭제되었습니다."));

            verify(chatService).deleteChatRoom(1L, 1L);
        }

        @Test
        @DisplayName("성공: 사용자 강퇴")
        void kickUser_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatService).kickUser(1L, 1L, 2L);

            // When + Then
            mockMvc.perform(delete("/api/chatrooms/1/kick/2")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("사용자가 강퇴되었습니다."));

            verify(chatService).kickUser(1L, 1L, 2L);
        }

        @Test
        @DisplayName("성공: 사용자 차단 해제")
        void unbanUser_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatService).unbanUser(1L, 1L, 2L);

            // When + Then
            mockMvc.perform(put("/api/chatrooms/1/unban/2")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방 사용자의 차단이 해제되었습니다."));

            verify(chatService).unbanUser(1L, 1L, 2L);
        }

        @Test
        @DisplayName("성공: 차단된 사용자 목록 조회")
        void getBannedUsers_Success() throws Exception {
            // Given
            ChatUserResponseDto bannedUser = ChatUserResponseDto.builder()
                    .id(3L)
                    .nickname("차단된사용자")
                    .thumbnailImageUrl("http://test.com/banned_thumb.png")
                    .build();

            List<ChatUserResponseDto> bannedUsers = Arrays.asList(bannedUser);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getBannedUsers(1L, 1L)).willReturn(bannedUsers);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/1/banned-users")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("차단된 사용자 목록을 성공적으로 조회했습니다."))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(3L))
                    .andExpect(jsonPath("$.data[0].nickname").value("차단된사용자"));

            verify(chatService).getBannedUsers(1L, 1L);
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 관리 작업")
        void managementAction_NotOwner() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            willThrow(InvalidChatRoomStateException.ownerRequired())
                    .given(chatService).kickUser(1L, 2L, 3L);

            // When + Then
            mockMvc.perform(delete("/api/chatrooms/1/kick/3")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }
    }

    @Nested
    @DisplayName("채팅방 검색 테스트")
    class SearchChatRoomsTest {

        @Test
        @DisplayName("성공: 키워드로 채팅방 검색")
        void searchChatRooms_Success() throws Exception {
            // Given
            String keyword = "테스트";
            List<ChatRoomResponseDto> searchResults = Arrays.asList(testChatRoomResponse);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.searchChatRooms(keyword, 1L)).willReturn(searchResults);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/search")
                            .header("Authorization", "Bearer test-token")
                            .param("keyword", keyword))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅방 검색이 완료되었습니다."))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(1L))
                    .andExpect(jsonPath("$.data[0].name").value("테스트 채팅방"));

            verify(chatService).searchChatRooms(keyword, 1L);
        }

        @Test
        @DisplayName("성공: 검색 결과 없음")
        void searchChatRooms_NoResults() throws Exception {
            // Given
            String keyword = "존재하지않는키워드";
            List<ChatRoomResponseDto> emptyResults = List.of();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.searchChatRooms(keyword, 1L)).willReturn(emptyResults);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/search")
                            .header("Authorization", "Bearer test-token")
                            .param("keyword", keyword))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("실패: 검색 키워드 누락")
        void searchChatRooms_MissingKeyword() throws Exception {
            // When + Then
            mockMvc.perform(get("/api/chatrooms/search")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("성공: 빈 키워드로 검색")
        void searchChatRooms_EmptyKeyword() throws Exception {
            // Given
            String emptyKeyword = "";
            List<ChatRoomResponseDto> emptyResults = List.of();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.searchChatRooms(emptyKeyword, 1L)).willReturn(emptyResults);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/search")
                            .header("Authorization", "Bearer test-token")
                            .param("keyword", emptyKeyword))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
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
            given(chatService.getChatRoomInfo(1L, 1L)).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/1")
                            .header("Authorization", "Bearer valid-token"))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(jwtTokenProvider).getUserIdFromToken("valid-token");
        }

        @Test
        @DisplayName("실패: Authorization 헤더 누락")
        void missingAuthorizationHeader() throws Exception {
            // When + Then
            mockMvc.perform(get("/api/chatrooms/1"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));
        }

        @Test
        @DisplayName("실패: 잘못된 JWT 토큰")
        void invalidJwtToken() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("invalid-token"))
                    .willThrow(new RuntimeException("Invalid token"));

            // When + Then
            mockMvc.perform(get("/api/chatrooms/1")
                            .header("Authorization", "Bearer invalid-token"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(500));
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
            given(chatService.getChatRoomInfo(1L, 1L)).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token"))
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
            List<ChatRoomResponseDto> chatRooms = Arrays.asList(testChatRoomResponse);
            Page<ChatRoomResponseDto> chatRoomPage = new PageImpl<>(chatRooms, PageRequest.of(0, 20), 100);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomsByUserId(1L, 0, 20)).willReturn(chatRoomPage);

            // When + Then
            mockMvc.perform(get("/api/chatrooms")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(100))
                    .andExpect(jsonPath("$.data.totalPages").value(5))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));
        }

        @Test
        @DisplayName("성공: 채팅방 상세 정보 포함")
        void chatRoomDetailIncluded() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomInfo(1L, 1L)).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.name").exists())
                    .andExpect(jsonPath("$.data.type").exists())
                    .andExpect(jsonPath("$.data.owner").exists())
                    .andExpect(jsonPath("$.data.users").isArray())
                    .andExpect(jsonPath("$.data.createdAt").exists());
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("채팅방 ID 경계값 - 최소값")
        void chatRoomId_MinValue() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomInfo(1L, 1L)).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("채팅방 ID 경계값 - 최대값")
        void chatRoomId_MaxValue() throws Exception {
            // Given
            Long maxChatRoomId = Long.MAX_VALUE;
            ChatRoomResponseDto maxResponse = ChatRoomResponseDto.builder()
                    .id(maxChatRoomId)
                    .name("최대ID채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testOwnerResponse)
                    .users(Arrays.asList(testOwnerResponse))
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomInfo(maxChatRoomId, 1L)).willReturn(maxResponse);

            // When + Then
            mockMvc.perform(get("/api/chatrooms/" + maxChatRoomId)
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(maxChatRoomId));
        }

        @Test
        @DisplayName("페이징 파라미터 경계값 - 최소값")
        void paging_MinValues() throws Exception {
            // Given
            Page<ChatRoomResponseDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomsByUserId(1L, 0, 1)).willReturn(emptyPage);

            // When + Then
            mockMvc.perform(get("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "0")
                            .param("size", "1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.size").value(1))
                    .andExpect(jsonPath("$.data.number").value(0));
        }

        @Test
        @DisplayName("페이징 파라미터 경계값 - 큰 값")
        void paging_LargeValues() throws Exception {
            // Given
            Page<ChatRoomResponseDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1000, 1000), 0);

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getChatRoomsByUserId(1L, 1000, 1000)).willReturn(emptyPage);

            // When + Then
            mockMvc.perform(get("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "1000")
                            .param("size", "1000"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.size").value(1000))
                    .andExpect(jsonPath("$.data.number").value(1000));
        }

        @Test
        @DisplayName("채팅방 이름 경계값 - 최소 길이")
        void chatRoomName_MinLength() throws Exception {
            // Given
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name("a") // 1자
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L))).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("채팅방 이름 경계값 - 최대 길이")
        void chatRoomName_MaxLength() throws Exception {
            // Given
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name("a".repeat(50)) // 50자
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L))).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("특수 문자가 포함된 채팅방 이름")
        void chatRoomName_SpecialCharacters() throws Exception {
            // Given
            String specialName = "테스트 채팅방 @#$%^&*()_+{}|:<>?[]\\;',./";
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name(specialName)
                    .build();

            ChatRoomResponseDto specialResponse = ChatRoomResponseDto.builder()
                    .id(1L)
                    .name(specialName)
                    .type(ChatRoomType.GROUP)
                    .owner(testOwnerResponse)
                    .users(Arrays.asList(testOwnerResponse))
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L))).willReturn(specialResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value(specialName));
        }

        @Test
        @DisplayName("유니코드 문자가 포함된 채팅방 이름")
        void chatRoomName_UnicodeCharacters() throws Exception {
            // Given
            String unicodeName = "테스트 채팅방 🎉😊🚀💬";
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name(unicodeName)
                    .build();

            ChatRoomResponseDto unicodeResponse = ChatRoomResponseDto.builder()
                    .id(1L)
                    .name(unicodeName)
                    .type(ChatRoomType.GROUP)
                    .owner(testOwnerResponse)
                    .users(Arrays.asList(testOwnerResponse))
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L))).willReturn(unicodeResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value(unicodeName));
        }

        @Test
        @DisplayName("사용자 ID 목록 경계값 - 빈 목록")
        void userIds_EmptyList() throws Exception {
            // Given
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name("빈 목록 채팅방")
                    .userIds(List.of())
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L))).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("사용자 ID 목록 경계값 - 매우 많은 사용자")
        void userIds_ManyUsers() throws Exception {
            // Given
            List<Long> manyUserIds = List.of(2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name("많은 사용자 채팅방")
                    .userIds(manyUserIds)
                    .build();

            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L))).willReturn(testChatRoomResponse);

            // When + Then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer test-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }
    }
}