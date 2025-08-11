package com.sonsminpark.auratalkback.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatUserResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.MessageType;
import com.sonsminpark.auratalkback.domain.chat.exception.ChatAccessDeniedException;
import com.sonsminpark.auratalkback.domain.chat.exception.ChatRoomNotFoundException;
import com.sonsminpark.auratalkback.domain.chat.exception.MessageNotFoundException;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.exception.GlobalExceptionHandler;
import com.sonsminpark.auratalkback.global.jwt.JwtTokenProvider;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController 테스트")
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private ChatController chatController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private ChatMessageResponseDto testMessageResponse;
    private ChatUserResponseDto testUserResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUserResponse = ChatUserResponseDto.builder()
                .id(1L)
                .nickname("테스트유저")
                .thumbnailImageUrl("http://test.com/thumb.png")
                .build();

        testMessageResponse = ChatMessageResponseDto.builder()
                .id(1L)
                .chatRoomId(1L)
                .sender(testUserResponse)
                .content("테스트 메시지")
                .type(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }

    @Nested
    @DisplayName("채팅 메시지 조회 테스트")
    class GetMessagesTest {

        @Test
        @DisplayName("성공: 채팅 메시지 목록 조회")
        void getMessages_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);

            List<ChatMessageResponseDto> messages = List.of(testMessageResponse);
            Page<ChatMessageResponseDto> messagePage = new PageImpl<>(messages, PageRequest.of(0, 50), 1);
            given(chatService.getMessages(eq(1L), eq(1L), any(Pageable.class))).willReturn(messagePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "0")
                            .param("size", "50"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("채팅 목록 조회 성공"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].id").value(1L))
                    .andExpect(jsonPath("$.data.content[0].content").value("테스트 메시지"))
                    .andExpect(jsonPath("$.data.content[0].type").value("TEXT"))
                    .andExpect(jsonPath("$.data.content[0].sender.nickname").value("테스트유저"));

            verify(jwtTokenProvider).getUserIdFromToken("test-token");
            verify(chatService).getMessages(eq(1L), eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("성공: 페이징 파라미터 기본값 적용")
        void getMessages_DefaultPaging() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);

            Page<ChatMessageResponseDto> messagePage = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
            given(chatService.getMessages(eq(1L), eq(1L), any(Pageable.class))).willReturn(messagePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.size").value(50))
                    .andExpect(jsonPath("$.data.number").value(0));
        }

        @Test
        @DisplayName("성공: 커스텀 페이징 파라미터")
        void getMessages_CustomPaging() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);

            Page<ChatMessageResponseDto> messagePage = new PageImpl<>(List.of(), PageRequest.of(2, 20), 0);
            given(chatService.getMessages(eq(1L), eq(1L), any(Pageable.class))).willReturn(messagePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer test-token")
                            .param("page", "2")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.number").value(2));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방")
        void getMessages_ChatRoomNotFound() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getMessages(eq(999L), eq(1L), any(Pageable.class)))
                    .willThrow(ChatRoomNotFoundException.of(999L));

            // When + Then
            mockMvc.perform(get("/api/chats/999")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(440));
        }

        @Test
        @DisplayName("실패: 채팅방 접근 권한 없음")
        void getMessages_AccessDenied() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            given(chatService.getMessages(eq(1L), eq(1L), any(Pageable.class)))
                    .willThrow(ChatAccessDeniedException.of("채팅방에 접근할 권한이 없습니다."));

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(441));
        }

        @Test
        @DisplayName("실패: 잘못된 채팅방 ID 형식")
        void getMessages_InvalidChatRoomId() throws Exception {
            // When + Then
            mockMvc.perform(get("/api/chats/invalid")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패: JWT 토큰 처리 오류")
        void getMessages_JwtTokenError() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("invalid-token"))
                    .willThrow(new RuntimeException("Invalid token"));

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer invalid-token"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("채팅 메시지 삭제 테스트")
    class DeleteMessageTest {

        @Test
        @DisplayName("성공: 메시지 삭제")
        void deleteMessage_Success() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willDoNothing().given(chatService).deleteMessage(1L, 1L);

            // When + Then
            mockMvc.perform(delete("/api/chats/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("메시지가 삭제되었습니다."))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(jwtTokenProvider).getUserIdFromToken("test-token");
            verify(chatService).deleteMessage(1L, 1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 메시지")
        void deleteMessage_MessageNotFound() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);
            willThrow(MessageNotFoundException.of(999L)).given(chatService).deleteMessage(999L, 1L);

            // When + Then
            mockMvc.perform(delete("/api/chats/999")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(442));
        }

        @Test
        @DisplayName("실패: 다른 사용자의 메시지 삭제 시도")
        void deleteMessage_NotOwner() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(2L);
            willThrow(MessageNotFoundException.of(1L)).given(chatService).deleteMessage(1L, 2L);

            // When + Then
            mockMvc.perform(delete("/api/chats/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(442));
        }

        @Test
        @DisplayName("실패: 잘못된 메시지 ID 형식")
        void deleteMessage_InvalidMessageId() throws Exception {
            // When + Then
            mockMvc.perform(delete("/api/chats/invalid")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
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

            Page<ChatMessageResponseDto> messagePage = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
            given(chatService.getMessages(eq(1L), eq(1L), any(Pageable.class))).willReturn(messagePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer valid-token"))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(jwtTokenProvider).getUserIdFromToken("valid-token");
        }

        @Test
        @DisplayName("실패: Authorization 헤더 누락")
        void missingAuthorizationHeader() throws Exception {
            // When + Then - 헤더가 없으면 NullPointerException 발생
            mockMvc.perform(get("/api/chats/1"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패: 잘못된 Authorization 헤더 형식 - Bearer 없음")
        void invalidAuthorizationHeaderFormat_NoBearer() throws Exception {
            // Given - "InvalidToken"을 substring(7)하면 "dToken"
            given(jwtTokenProvider.getUserIdFromToken("dToken"))
                    .willThrow(new RuntimeException("Invalid token format"));

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "InvalidToken"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패: 짧은 Authorization 헤더")
        void shortAuthorizationHeader() throws Exception {
            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "short"))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패: Bearer만 있고 토큰 없음")
        void bearerWithoutToken() throws Exception {
            // Given - "Bearer "에서 substring(7)하면 빈 문자열
            given(jwtTokenProvider.getUserIdFromToken("")).willThrow(new RuntimeException("Empty token"));

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer "))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("실패: Bearer 이후 공백만 있는 경우")
        void bearerWithSpaces() throws Exception {
            // Given
            given(jwtTokenProvider.getUserIdFromToken(" ")).willThrow(new RuntimeException("Invalid token"));

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer  "))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());
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

            Page<ChatMessageResponseDto> messagePage = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
            given(chatService.getMessages(eq(1L), eq(1L), any(Pageable.class))).willReturn(messagePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1")
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
            given(jwtTokenProvider.getUserIdFromToken("test-token")).willReturn(1L);

            Page<ChatMessageResponseDto> messagePage = new PageImpl<>(
                    List.of(testMessageResponse),
                    PageRequest.of(0, 50),
                    100
            );
            given(chatService.getMessages(eq(1L), eq(1L), any(Pageable.class))).willReturn(messagePage);

            // When + Then
            mockMvc.perform(get("/api/chats/1")
                            .header("Authorization", "Bearer test-token"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(100))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.size").value(50))
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));
        }
    }
}