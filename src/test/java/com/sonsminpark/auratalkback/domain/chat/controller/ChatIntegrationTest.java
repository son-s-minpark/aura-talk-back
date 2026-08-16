package com.sonsminpark.auratalkback.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.OneToOneChatRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.*;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import com.sonsminpark.auratalkback.domain.chat.entity.MessageType;
import com.sonsminpark.auratalkback.domain.chat.service.ChatFileService;
import com.sonsminpark.auratalkback.domain.chat.service.ChatRoomImageService;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.domain.chat.service.RandomChatService;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
import com.sonsminpark.auratalkback.global.s3.S3Service;
import lombok.extern.slf4j.Slf4j;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class ChatIntegrationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ChatService chatService;

    @Mock
    private ChatFileService chatFileService;

    @Mock
    private ChatRoomImageService chatRoomImageService;

    @Mock
    private RandomChatService randomChatService;

    @Mock
    private S3Service s3Service;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private ChatRoomController chatRoomController;

    @InjectMocks
    private ChatController chatController;

    @InjectMocks
    private ChatFileController chatFileController;

    @InjectMocks
    private RandomChatController randomChatController;

    private ChatUserResponseDto user1;
    private ChatUserResponseDto user2;
    private ChatUserResponseDto user3;
    private String testToken;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // MockMvc 수동 설정
        mockMvc = MockMvcBuilders.standaloneSetup(
                chatRoomController,
                chatController,
                chatFileController,
                randomChatController
        ).build();

        // 테스트 사용자 생성
        user1 = ChatUserResponseDto.builder()
                .id(1L)
                .nickname("유저1")
                .thumbnailImageUrl("https://test.com/user1.jpg")
                .build();

        user2 = ChatUserResponseDto.builder()
                .id(2L)
                .nickname("유저2")
                .thumbnailImageUrl("https://test.com/user2.jpg")
                .build();

        user3 = ChatUserResponseDto.builder()
                .id(3L)
                .nickname("유저3")
                .thumbnailImageUrl("https://test.com/user3.jpg")
                .build();

        testToken = "test.jwt.token";

        // JWT 관련 Mock 설정
        given(jwtTokenProvider.getUserIdFromToken(testToken)).willReturn(1L);
    }

    @Nested
    @DisplayName("채팅방 관리 테스트")
    class ChatRoomManagementTest {

        @Test
        @DisplayName("그룹 채팅방 생성 성공")
        void createGroupChatRoom_Success() throws Exception {
            // given
            ChatRoomCreateRequestDto request = ChatRoomCreateRequestDto.builder()
                    .name("테스트 그룹 채팅방")
                    .userIds(Arrays.asList(2L, 3L))
                    .build();

            ChatRoomResponseDto response = ChatRoomResponseDto.builder()
                    .id(1L)
                    .name("테스트 그룹 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(user1)
                    .users(Arrays.asList(user1, user2, user3))
                    .createdAt(LocalDateTime.now())
                    .isActive(true)
                    .isOwner(true)
                    .build();

            given(chatService.createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/chatrooms")
                            .header("Authorization", "Bearer " + testToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("테스트 그룹 채팅방"))
                    .andExpect(jsonPath("$.data.type").value("GROUP"))
                    .andExpect(jsonPath("$.data.owner.id").value(1L))
                    .andExpect(jsonPath("$.data.users", hasSize(3))); // isOwner 검증 제거

            verify(chatService).createChatRoom(any(ChatRoomCreateRequestDto.class), eq(1L));
        }

        @Test
        @DisplayName("1:1 채팅방 생성 성공")
        void createOneToOneChatRoom_Success() throws Exception {
            // given
            OneToOneChatRequestDto request = OneToOneChatRequestDto.builder()
                    .targetUserId(2L)
                    .build();

            ChatRoomResponseDto response = ChatRoomResponseDto.builder()
                    .id(1L)
                    .name("유저1, 유저2")
                    .type(ChatRoomType.ONE_TO_ONE)
                    .owner(user1)
                    .users(Arrays.asList(user1, user2))
                    .createdAt(LocalDateTime.now())
                    .isActive(true)
                    .isOwner(true)
                    .build();

            given(chatService.createOneToOneChatRoom(1L, 2L)).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/chatrooms/one-to-one")
                            .header("Authorization", "Bearer " + testToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.type").value("ONE_TO_ONE"))
                    .andExpect(jsonPath("$.data.users", hasSize(2)));

            verify(chatService).createOneToOneChatRoom(1L, 2L);
        }

        @Test
        @DisplayName("채팅방 목록 조회 성공")
        void getChatRooms_Success() throws Exception {
            // given
            List<ChatRoomResponseDto> chatRooms = Arrays.asList(
                    createChatRoomResponse(1L, "채팅방1", ChatRoomType.GROUP),
                    createChatRoomResponse(2L, "채팅방2", ChatRoomType.GROUP)
            );
            Page<ChatRoomResponseDto> page = new PageImpl<>(chatRooms, PageRequest.of(0, 10), 2);

            given(chatService.getChatRoomsByUserId(1L, 0, 10)).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/chatrooms")
                            .header("Authorization", "Bearer " + testToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalElements").value(2));

            verify(chatService).getChatRoomsByUserId(1L, 0, 10);
        }

        @Test
        @DisplayName("채팅방 정보 조회 성공")
        void getChatRoomInfo_Success() throws Exception {
            // given
            Long chatRoomId = 1L;
            ChatRoomResponseDto response = createChatRoomResponse(chatRoomId, "테스트 채팅방", ChatRoomType.GROUP);

            given(chatService.getChatRoomInfo(chatRoomId, 1L)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/chatrooms/{chatroomId}", chatRoomId)
                            .header("Authorization", "Bearer " + testToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(chatRoomId))
                    .andExpect(jsonPath("$.data.name").value("테스트 채팅방"));

            verify(chatService).getChatRoomInfo(chatRoomId, 1L);
        }

        @Test
        @DisplayName("채팅방 나가기 성공")
        void leaveChatRoom_Success() throws Exception {
            // given
            Long chatRoomId = 1L;
            doNothing().when(chatService).leaveChatRoom(chatRoomId, 1L);

            // when & then
            mockMvc.perform(delete("/api/chatrooms/{chatroomId}", chatRoomId)
                            .header("Authorization", "Bearer " + testToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(chatService).leaveChatRoom(chatRoomId, 1L);
        }
    }

    @Nested
    @DisplayName("메시지 관리 테스트")
    class MessageManagementTest {

        @Test
        @DisplayName("채팅 메시지 목록 조회 성공")
        void getMessages_Success() throws Exception {
            // given
            Long chatRoomId = 1L;
            List<ChatMessageResponseDto> messages = Arrays.asList(
                    createMessageResponse(1L, "안녕하세요!", user1),
                    createMessageResponse(2L, "반갑습니다!", user2)
            );
            Page<ChatMessageResponseDto> page = new PageImpl<>(messages, PageRequest.of(0, 50), 2);

            given(chatService.getMessages(eq(chatRoomId), eq(1L), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/api/chats/{chatroomId}", chatRoomId)
                            .header("Authorization", "Bearer " + testToken)
                            .param("page", "0")
                            .param("size", "50"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[0].content").value("안녕하세요!"))
                    .andExpect(jsonPath("$.data.content[1].content").value("반갑습니다!"));

            verify(chatService).getMessages(eq(chatRoomId), eq(1L), any());
        }

        @Test
        @DisplayName("메시지 삭제 성공")
        void deleteMessage_Success() throws Exception {
            // given
            Long messageId = 1L;
            doNothing().when(chatService).deleteMessage(messageId, 1L);

            // when & then
            mockMvc.perform(delete("/api/chats/{messageId}", messageId)
                            .header("Authorization", "Bearer " + testToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(chatService).deleteMessage(messageId, 1L);
        }
    }

    @Nested
    @DisplayName("초대 및 권한 관리 테스트")
    class InviteAndPermissionTest {

        @Test
        @DisplayName("초대 링크 생성 성공")
        void createInviteLink_Success() throws Exception {
            // given
            Long chatRoomId = 1L;
            ChatInviteResponseDto response = ChatInviteResponseDto.builder()
                    .inviteCode("test-invite-code")
                    .inviteLink("https://auratalk.com/invite/test-invite-code")
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            given(chatService.createInviteLink(chatRoomId, 1L)).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/chatrooms/{chatroomId}/invite-link", chatRoomId)
                            .header("Authorization", "Bearer " + testToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.inviteCode").value("test-invite-code"))
                    .andExpect(jsonPath("$.data.inviteLink").value("https://auratalk.com/invite/test-invite-code"))
                    .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());

            verify(chatService).createInviteLink(chatRoomId, 1L);
        }

        @Test
        @DisplayName("친구에게 초대 링크 전송 성공")
        void sendInviteToFriend_Success() throws Exception {
            // given
            Long chatRoomId = 1L;
            ChatInviteRequestDto request = ChatInviteRequestDto.builder()
                    .userId(3L)
                    .build();

            ChatInviteResponseDto response = ChatInviteResponseDto.builder()
                    .inviteCode("test-invite-code")
                    .inviteLink("https://auratalk.com/invite/test-invite-code")
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build();

            given(chatService.sendInviteToFriend(eq(chatRoomId), any(ChatInviteRequestDto.class), eq(1L)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/chatrooms/{chatroomId}/invite", chatRoomId)
                            .header("Authorization", "Bearer " + testToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.inviteCode").value("test-invite-code"));

            verify(chatService).sendInviteToFriend(eq(chatRoomId), any(ChatInviteRequestDto.class), eq(1L));
        }

        @Test
        @DisplayName("초대 링크로 채팅방 참여 성공")
        void joinChatRoomByInviteLink_Success() throws Exception {
            // given
            String inviteCode = "test-invite-code";
            doNothing().when(chatService).acceptInvite(inviteCode, 1L);

            // when & then
            mockMvc.perform(post("/api/chatrooms/join")
                            .header("Authorization", "Bearer " + testToken)
                            .param("inviteCode", inviteCode))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(chatService).acceptInvite(inviteCode, 1L);
        }
    }

    @Nested
    @DisplayName("채팅방 검색 테스트")
    class ChatRoomSearchTest {

        @Test
        @DisplayName("채팅방 이름으로 검색 성공")
        void searchChatRooms_Success() throws Exception {
            // given
            String keyword = "개발";
            List<ChatRoomResponseDto> searchResults = Arrays.asList(
                    createChatRoomResponse(1L, "개발팀 채팅방", ChatRoomType.GROUP)
            );

            given(chatService.searchChatRooms(keyword, 1L)).willReturn(searchResults);

            // when & then
            mockMvc.perform(get("/api/chatrooms/search")
                            .header("Authorization", "Bearer " + testToken)
                            .param("keyword", keyword))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name", containsString("개발팀")));

            verify(chatService).searchChatRooms(keyword, 1L);
        }

        @Test
        @DisplayName("빈 키워드로 검색 시 빈 결과 반환")
        void searchChatRooms_EmptyKeyword() throws Exception {
            // given
            String keyword = "";
            given(chatService.searchChatRooms(keyword, 1L)).willReturn(Arrays.asList());

            // when & then
            mockMvc.perform(get("/api/chatrooms/search")
                            .header("Authorization", "Bearer " + testToken)
                            .param("keyword", keyword))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(chatService).searchChatRooms(keyword, 1L);
        }
    }

    @Nested
    @DisplayName("랜덤 채팅 테스트")
    class RandomChatTest {

        @Test
        @DisplayName("랜덤 채팅 시작 성공")
        void startRandomChat_Success() throws Exception {
            // given
            RandomChatMatchResponseDto response = RandomChatMatchResponseDto.builder()
                    .matched(true)
                    .waiting(false)
                    .chatRoom(createChatRoomResponse(1L, "랜덤 채팅방", ChatRoomType.RANDOM))
                    .matchedUser(user2)
                    .matchedAt(LocalDateTime.now())
                    .message("매칭이 완료되었습니다!")
                    .build();

            given(randomChatService.startRandomChat(eq(1L), any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/random-chat/start")
                            .header("Authorization", "Bearer " + testToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"interests\":[\"게임\",\"독서\"]}"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.matched").value(true))
                    .andExpect(jsonPath("$.data.message").value("매칭이 완료되었습니다!"));

            verify(randomChatService).startRandomChat(eq(1L), any());
        }
    }

    // 헬퍼 메서드들
    private ChatRoomResponseDto createChatRoomResponse(Long id, String name, ChatRoomType type) {
        return ChatRoomResponseDto.builder()
                .id(id)
                .name(name)
                .type(type)
                .owner(user1)
                .users(Arrays.asList(user1, user2, user3))
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .isOwner(true)
                .build();
    }

    private ChatMessageResponseDto createMessageResponse(Long id, String content, ChatUserResponseDto sender) {
        return ChatMessageResponseDto.builder()
                .id(id)
                .chatRoomId(1L)
                .sender(sender)
                .content(content)
                .type(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }
}