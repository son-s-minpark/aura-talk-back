package com.sonsminpark.auratalkback.domain.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatMessageResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatUserResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.MessageType;
import com.sonsminpark.auratalkback.domain.chat.exception.ChatAccessDeniedException;
import com.sonsminpark.auratalkback.domain.chat.exception.ChatRoomNotFoundException;
import com.sonsminpark.auratalkback.domain.chat.exception.InvalidChatRoomStateException;
import com.sonsminpark.auratalkback.domain.chat.service.ChatService;
import com.sonsminpark.auratalkback.global.websocket.WebSocketUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatWebSocketController 테스트")
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    private ObjectMapper objectMapper;
    private WebSocketUser testWebSocketUser;
    private ChatMessageRequestDto testMessageRequest;
    private ChatMessageResponseDto testMessageResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testWebSocketUser = new WebSocketUser("test@example.com", 1L);

        testMessageRequest = ChatMessageRequestDto.builder()
                .content("테스트 메시지")
                .type(MessageType.TEXT)
                .build();

        ChatUserResponseDto senderDto = ChatUserResponseDto.builder()
                .id(1L)
                .nickname("테스트유저")
                .thumbnailImageUrl("http://test.com/thumb.png")
                .build();

        testMessageResponse = ChatMessageResponseDto.builder()
                .id(1L)
                .chatRoomId(1L)
                .sender(senderDto)
                .content("테스트 메시지")
                .type(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();
    }

    @Nested
    @DisplayName("WebSocket 메시지 전송 테스트")
    class SendMessageTest {

        @Test
        @DisplayName("성공: 텍스트 메시지 전송")
        void sendMessage_TextMessage_Success() {
            // Given
            Long chatroomId = 1L;
            given(chatService.sendMessage(chatroomId, testMessageRequest, 1L))
                    .willReturn(testMessageResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, testMessageRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getContent()).isEqualTo("테스트 메시지");
            assertThat(result.getType()).isEqualTo(MessageType.TEXT);
            assertThat(result.getSender().getId()).isEqualTo(1L);

            verify(chatService).sendMessage(chatroomId, testMessageRequest, 1L);
        }

        @Test
        @DisplayName("성공: 이미지 메시지 전송")
        void sendMessage_ImageMessage_Success() {
            // Given
            Long chatroomId = 1L;
            ChatMessageRequestDto imageRequest = ChatMessageRequestDto.builder()
                    .content("image.jpg")
                    .type(MessageType.IMAGE)
                    .build();

            ChatMessageResponseDto imageResponse = ChatMessageResponseDto.builder()
                    .id(2L)
                    .chatRoomId(1L)
                    .sender(testMessageResponse.getSender())
                    .content("image.jpg")
                    .type(MessageType.IMAGE)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, imageRequest, 1L))
                    .willReturn(imageResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, imageRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(MessageType.IMAGE);
            assertThat(result.getContent()).isEqualTo("image.jpg");

            verify(chatService).sendMessage(chatroomId, imageRequest, 1L);
        }

        @Test
        @DisplayName("성공: 파일 메시지 전송")
        void sendMessage_FileMessage_Success() {
            // Given
            Long chatroomId = 1L;
            ChatMessageRequestDto fileRequest = ChatMessageRequestDto.builder()
                    .content("document.pdf")
                    .type(MessageType.FILE)
                    .build();

            ChatMessageResponseDto fileResponse = ChatMessageResponseDto.builder()
                    .id(3L)
                    .chatRoomId(1L)
                    .sender(testMessageResponse.getSender())
                    .content("document.pdf")
                    .type(MessageType.FILE)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, fileRequest, 1L))
                    .willReturn(fileResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, fileRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(MessageType.FILE);
            assertThat(result.getContent()).isEqualTo("document.pdf");

            verify(chatService).sendMessage(chatroomId, fileRequest, 1L);
        }

        @Test
        @DisplayName("성공: 시스템 메시지 전송")
        void sendMessage_SystemMessage_Success() {
            // Given
            Long chatroomId = 1L;
            ChatMessageRequestDto systemRequest = ChatMessageRequestDto.builder()
                    .content("사용자가 입장했습니다.")
                    .type(MessageType.SYSTEM)
                    .build();

            ChatMessageResponseDto systemResponse = ChatMessageResponseDto.builder()
                    .id(4L)
                    .chatRoomId(1L)
                    .sender(null) // 시스템 메시지
                    .content("사용자가 입장했습니다.")
                    .type(MessageType.SYSTEM)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, systemRequest, 1L))
                    .willReturn(systemResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, systemRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(MessageType.SYSTEM);
            assertThat(result.getSender()).isNull();

            verify(chatService).sendMessage(chatroomId, systemRequest, 1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방")
        void sendMessage_ChatRoomNotFound() {
            // Given
            Long invalidChatroomId = 999L;
            given(chatService.sendMessage(invalidChatroomId, testMessageRequest, 1L))
                    .willThrow(ChatRoomNotFoundException.of(invalidChatroomId));

            // When + Then
            assertThatThrownBy(() -> chatWebSocketController.sendMessage(
                    invalidChatroomId, testMessageRequest, testWebSocketUser))
                    .isInstanceOf(ChatRoomNotFoundException.class);

            verify(chatService).sendMessage(invalidChatroomId, testMessageRequest, 1L);
        }

        @Test
        @DisplayName("실패: 채팅방 접근 권한 없음")
        void sendMessage_AccessDenied() {
            // Given
            Long chatroomId = 1L;
            given(chatService.sendMessage(chatroomId, testMessageRequest, 1L))
                    .willThrow(ChatAccessDeniedException.of("채팅방에 접근할 권한이 없습니다."));

            // When + Then
            assertThatThrownBy(() -> chatWebSocketController.sendMessage(
                    chatroomId, testMessageRequest, testWebSocketUser))
                    .isInstanceOf(ChatAccessDeniedException.class);

            verify(chatService).sendMessage(chatroomId, testMessageRequest, 1L);
        }

        @Test
        @DisplayName("실패: 비활성화된 채팅방")
        void sendMessage_InactiveChatRoom() {
            // Given
            Long chatroomId = 1L;
            given(chatService.sendMessage(chatroomId, testMessageRequest, 1L))
                    .willThrow(InvalidChatRoomStateException.deactivated());

            // When + Then
            assertThatThrownBy(() -> chatWebSocketController.sendMessage(
                    chatroomId, testMessageRequest, testWebSocketUser))
                    .isInstanceOf(InvalidChatRoomStateException.class);

            verify(chatService).sendMessage(chatroomId, testMessageRequest, 1L);
        }

        @Test
        @DisplayName("실패: 유효하지 않은 Principal")
        void sendMessage_InvalidPrincipal() {
            // Given
            Principal invalidPrincipal = () -> "invalid-user";
            Long chatroomId = 1L;

            // When + Then
            assertThatThrownBy(() -> chatWebSocketController.sendMessage(
                    chatroomId, testMessageRequest, invalidPrincipal))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증된 사용자가 아닙니다.");

            verify(chatService, never()).sendMessage(any(), any(), any());
        }

        @Test
        @DisplayName("실패: null Principal")
        void sendMessage_NullPrincipal() {
            // Given
            Principal nullPrincipal = null;
            Long chatroomId = 1L;

            // When + Then
            assertThatThrownBy(() -> chatWebSocketController.sendMessage(
                    chatroomId, testMessageRequest, nullPrincipal))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("인증된 사용자가 아닙니다.");

            verify(chatService, never()).sendMessage(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("WebSocket 예외 처리 테스트")
    class HandleExceptionTest {

        @Test
        @DisplayName("성공: 일반 예외 처리")
        void handleException_GeneralException() {
            // Given
            Exception testException = new RuntimeException("테스트 예외");

            // When
            String result = chatWebSocketController.handleException(testException, testWebSocketUser);

            // Then
            assertThat(result).isEqualTo("메시지 전송에 실패했습니다: 테스트 예외");

            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"),
                    eq("/queue/errors"),
                    eq("메시지 전송에 실패했습니다: 테스트 예외")
            );
        }

        @Test
        @DisplayName("성공: 채팅방 관련 예외 처리")
        void handleException_ChatRoomException() {
            // Given
            ChatRoomNotFoundException testException = ChatRoomNotFoundException.of(1L);

            // When
            String result = chatWebSocketController.handleException(testException, testWebSocketUser);

            // Then
            assertThat(result).contains("메시지 전송에 실패했습니다");
            assertThat(result).contains("채팅방을 찾을 수 없습니다");

            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"),
                    eq("/queue/errors"),
                    anyString()
            );
        }

        @Test
        @DisplayName("성공: 접근 권한 예외 처리")
        void handleException_AccessDeniedException() {
            // Given
            ChatAccessDeniedException testException = ChatAccessDeniedException.of("권한이 없습니다.");

            // When
            String result = chatWebSocketController.handleException(testException, testWebSocketUser);

            // Then
            assertThat(result).contains("메시지 전송에 실패했습니다");
            assertThat(result).contains("권한이 없습니다.");

            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"),
                    eq("/queue/errors"),
                    anyString()
            );
        }

        @Test
        @DisplayName("성공: null Principal로 예외 처리")
        void handleException_NullPrincipal() {
            // Given
            Exception testException = new RuntimeException("테스트 예외");
            Principal nullPrincipal = null;

            // When
            String result = chatWebSocketController.handleException(testException, nullPrincipal);

            // Then
            assertThat(result).isEqualTo("메시지 전송에 실패했습니다: 테스트 예외");

            // null Principal인 경우 메시지 전송하지 않음
            verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("성공: 유효하지 않은 Principal로 예외 처리")
        void handleException_InvalidPrincipal() {
            // Given
            Exception testException = new RuntimeException("테스트 예외");
            Principal invalidPrincipal = () -> "invalid-user";

            // When
            String result = chatWebSocketController.handleException(testException, invalidPrincipal);

            // Then
            assertThat(result).isEqualTo("메시지 전송에 실패했습니다: 테스트 예외");

            // invalid Principal인 경우에는 메시지 전송 시도함 (unknown 사용자 ID로)
            verify(messagingTemplate).convertAndSendToUser(
                    eq("unknown"),
                    eq("/queue/errors"),
                    anyString()
            );
        }

        @Test
        @DisplayName("성공: 메시지가 null인 예외 처리")
        void handleException_NullMessage() {
            // Given
            Exception testException = new RuntimeException(); // 메시지가 null

            // When
            String result = chatWebSocketController.handleException(testException, testWebSocketUser);

            // Then
            assertThat(result).contains("메시지 전송에 실패했습니다");
            assertThat(result).contains("null");

            verify(messagingTemplate).convertAndSendToUser(
                    eq("1"),
                    eq("/queue/errors"),
                    anyString()
            );
        }
    }

    @Nested
    @DisplayName("메시지 타입별 처리 테스트")
    class MessageTypeTest {

        @Test
        @DisplayName("성공: 음성 메시지 전송")
        void sendMessage_VoiceMessage_Success() {
            // Given
            Long chatroomId = 1L;
            ChatMessageRequestDto voiceRequest = ChatMessageRequestDto.builder()
                    .content("voice-message.mp3")
                    .type(MessageType.VOICE)
                    .build();

            ChatMessageResponseDto voiceResponse = ChatMessageResponseDto.builder()
                    .id(5L)
                    .chatRoomId(1L)
                    .sender(testMessageResponse.getSender())
                    .content("voice-message.mp3")
                    .type(MessageType.VOICE)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, voiceRequest, 1L))
                    .willReturn(voiceResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, voiceRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(MessageType.VOICE);
            assertThat(result.getContent()).isEqualTo("voice-message.mp3");

            verify(chatService).sendMessage(chatroomId, voiceRequest, 1L);
        }

        @Test
        @DisplayName("성공: 비디오 메시지 전송")
        void sendMessage_VideoMessage_Success() {
            // Given
            Long chatroomId = 1L;
            ChatMessageRequestDto videoRequest = ChatMessageRequestDto.builder()
                    .content("video.mp4")
                    .type(MessageType.VIDEO)
                    .build();

            ChatMessageResponseDto videoResponse = ChatMessageResponseDto.builder()
                    .id(6L)
                    .chatRoomId(1L)
                    .sender(testMessageResponse.getSender())
                    .content("video.mp4")
                    .type(MessageType.VIDEO)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, videoRequest, 1L))
                    .willReturn(videoResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, videoRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(MessageType.VIDEO);
            assertThat(result.getContent()).isEqualTo("video.mp4");

            verify(chatService).sendMessage(chatroomId, videoRequest, 1L);
        }

        @Test
        @DisplayName("성공: 초대 메시지 전송")
        void sendMessage_InviteMessage_Success() {
            // Given
            Long chatroomId = 1L;
            ChatMessageRequestDto inviteRequest = ChatMessageRequestDto.builder()
                    .content("https://auratalk.com/invite/abc123")
                    .type(MessageType.INVITE)
                    .build();

            ChatMessageResponseDto inviteResponse = ChatMessageResponseDto.builder()
                    .id(7L)
                    .chatRoomId(1L)
                    .sender(testMessageResponse.getSender())
                    .content("https://auratalk.com/invite/abc123")
                    .type(MessageType.INVITE)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, inviteRequest, 1L))
                    .willReturn(inviteResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, inviteRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(MessageType.INVITE);
            assertThat(result.getContent()).isEqualTo("https://auratalk.com/invite/abc123");

            verify(chatService).sendMessage(chatroomId, inviteRequest, 1L);
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("채팅방 ID 경계값 - 최소값")
        void sendMessage_MinChatroomId() {
            // Given
            Long minChatroomId = 1L;
            given(chatService.sendMessage(minChatroomId, testMessageRequest, 1L))
                    .willReturn(testMessageResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    minChatroomId, testMessageRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            verify(chatService).sendMessage(minChatroomId, testMessageRequest, 1L);
        }

        @Test
        @DisplayName("채팅방 ID 경계값 - 최대값")
        void sendMessage_MaxChatroomId() {
            // Given
            Long maxChatroomId = Long.MAX_VALUE;
            ChatMessageResponseDto maxResponse = ChatMessageResponseDto.builder()
                    .id(1L)
                    .chatRoomId(maxChatroomId)
                    .sender(testMessageResponse.getSender())
                    .content("테스트 메시지")
                    .type(MessageType.TEXT)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(maxChatroomId, testMessageRequest, 1L))
                    .willReturn(maxResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    maxChatroomId, testMessageRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getChatRoomId()).isEqualTo(maxChatroomId);
            verify(chatService).sendMessage(maxChatroomId, testMessageRequest, 1L);
        }

        @Test
        @DisplayName("메시지 내용 경계값 - 빈 문자열")
        void sendMessage_EmptyContent() {
            // Given
            Long chatroomId = 1L;
            ChatMessageRequestDto emptyRequest = ChatMessageRequestDto.builder()
                    .content("")
                    .type(MessageType.TEXT)
                    .build();

            ChatMessageResponseDto emptyResponse = ChatMessageResponseDto.builder()
                    .id(1L)
                    .chatRoomId(1L)
                    .sender(testMessageResponse.getSender())
                    .content("")
                    .type(MessageType.TEXT)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, emptyRequest, 1L))
                    .willReturn(emptyResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, emptyRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            verify(chatService).sendMessage(chatroomId, emptyRequest, 1L);
        }

        @Test
        @DisplayName("메시지 내용 경계값 - 매우 긴 문자열")
        void sendMessage_VeryLongContent() {
            // Given
            Long chatroomId = 1L;
            String longContent = "a".repeat(10000);
            ChatMessageRequestDto longRequest = ChatMessageRequestDto.builder()
                    .content(longContent)
                    .type(MessageType.TEXT)
                    .build();

            ChatMessageResponseDto longResponse = ChatMessageResponseDto.builder()
                    .id(1L)
                    .chatRoomId(1L)
                    .sender(testMessageResponse.getSender())
                    .content(longContent)
                    .type(MessageType.TEXT)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, longRequest, 1L))
                    .willReturn(longResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, longRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(10000);
            verify(chatService).sendMessage(chatroomId, longRequest, 1L);
        }

        @Test
        @DisplayName("사용자 ID 경계값 - 최대값")
        void sendMessage_MaxUserId() {
            // Given
            Long chatroomId = 1L;
            Long maxUserId = Long.MAX_VALUE;
            WebSocketUser maxUserIdUser = new WebSocketUser("max@test.com", maxUserId);

            ChatUserResponseDto maxUserDto = ChatUserResponseDto.builder()
                    .id(maxUserId)
                    .nickname("최대ID유저")
                    .build();

            ChatMessageResponseDto maxUserResponse = ChatMessageResponseDto.builder()
                    .id(1L)
                    .chatRoomId(1L)
                    .sender(maxUserDto)
                    .content("테스트 메시지")
                    .type(MessageType.TEXT)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, testMessageRequest, maxUserId))
                    .willReturn(maxUserResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, testMessageRequest, maxUserIdUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getSender().getId()).isEqualTo(maxUserId);
            verify(chatService).sendMessage(chatroomId, testMessageRequest, maxUserId);
        }

        @Test
        @DisplayName("특수 문자가 포함된 메시지 내용")
        void sendMessage_SpecialCharacters() {
            // Given
            Long chatroomId = 1L;
            String specialContent = "테스트 메시지 🎉😊 @#$%^&*()_+{}|:<>?[]\\;',./";
            ChatMessageRequestDto specialRequest = ChatMessageRequestDto.builder()
                    .content(specialContent)
                    .type(MessageType.TEXT)
                    .build();

            ChatMessageResponseDto specialResponse = ChatMessageResponseDto.builder()
                    .id(1L)
                    .chatRoomId(1L)
                    .sender(testMessageResponse.getSender())
                    .content(specialContent)
                    .type(MessageType.TEXT)
                    .createdAt(LocalDateTime.now())
                    .isDeleted(false)
                    .build();

            given(chatService.sendMessage(chatroomId, specialRequest, 1L))
                    .willReturn(specialResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, specialRequest, testWebSocketUser);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(specialContent);
            verify(chatService).sendMessage(chatroomId, specialRequest, 1L);
        }

        @Test
        @DisplayName("서비스에서 예외 발생 시 예외 처리 흐름")
        void sendMessage_ServiceException_HandledCorrectly() {
            // Given
            Long chatroomId = 1L;
            RuntimeException serviceException = new RuntimeException("서비스 에러");
            given(chatService.sendMessage(chatroomId, testMessageRequest, 1L))
                    .willThrow(serviceException);

            // When + Then
            assertThatThrownBy(() -> chatWebSocketController.sendMessage(
                    chatroomId, testMessageRequest, testWebSocketUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("서비스 에러");

            verify(chatService).sendMessage(chatroomId, testMessageRequest, 1L);
        }

        @Test
        @DisplayName("메시지 응답에서 필수 필드들이 제대로 설정되는지 확인")
        void sendMessage_ResponseFieldsValidation() {
            // Given
            Long chatroomId = 1L;
            given(chatService.sendMessage(chatroomId, testMessageRequest, 1L))
                    .willReturn(testMessageResponse);

            // When
            ChatMessageResponseDto result = chatWebSocketController.sendMessage(
                    chatroomId, testMessageRequest, testWebSocketUser);

            // Then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getChatRoomId()).isNotNull();
            assertThat(result.getSender()).isNotNull();
            assertThat(result.getContent()).isNotNull();
            assertThat(result.getType()).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.isDeleted()).isFalse();
        }
    }
}