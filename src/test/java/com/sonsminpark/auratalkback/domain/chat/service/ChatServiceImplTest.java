package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomUpdateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.*;
import com.sonsminpark.auratalkback.domain.chat.entity.*;
import com.sonsminpark.auratalkback.domain.chat.exception.*;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatInvitationRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatMessageRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.user.dto.response.ProfileImageResponseDto;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserProfileImage;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
import com.sonsminpark.auratalkback.domain.user.service.UserProfileImageService;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 테스트")
class ChatServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomUserRepository chatRoomUserRepository;

    @Mock
    private ChatInvitationRepository chatInvitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserProfileImageService userProfileImageService;

    @Mock
    private ChatRoomImageService chatRoomImageService;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private ChatRoom testChatRoom;
    private ChatMessage testMessage;
    private ChatMessage testSystemMessage;
    private ChatRoomUser testRoomUser;
    private ChatRoomUser testRoomUser2;
    private ProfileImageResponseDto testProfileImage;
    private ChatInvitation testInvitation;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "bucketName", "test-bucket");

        testUser1 = createTestUser(1L, "test1@example.com", "testuser1", "테스트유저1");
        testUser2 = createTestUser(2L, "test2@example.com", "testuser2", "테스트유저2");
        testUser3 = createTestUser(3L, "test3@example.com", "testuser3", "테스트유저3");

        testChatRoom = createTestChatRoom();

        testMessage = createTestMessage();
        testSystemMessage = createTestSystemMessage();

        testRoomUser = createTestChatRoomUser(testChatRoom, testUser1);
        testRoomUser2 = createTestChatRoomUser(testChatRoom, testUser2);

        testProfileImage = createTestProfileImage();

        testInvitation = createTestInvitation();
    }

    private User createTestUser(Long id, String email, String username, String nickname) {
        User user = User.builder()
                .id(id)
                .email(email)
                .username(username)
                .nickname(nickname)
                .password("encodedPassword")
                .status(UserStatus.ONLINE)
                .isDeleted(false)
                .emailVerified(true)
                .build();

        UserProfileImage profileImage = UserProfileImage.builder()
                .userId(id)
                .originalImageUrl("http://test.com/profile" + id + ".png")
                .thumbnailImageUrl("http://test.com/profile" + id + "_thumb.png")
                .isDefaultProfileImage(false)
                .build();

        ReflectionTestUtils.setField(user, "userProfileImage", profileImage);
        return user;
    }

    private ChatRoom createTestChatRoom() {
        return ChatRoom.builder()
                .id(1L)
                .name("테스트 채팅방")
                .type(ChatRoomType.GROUP)
                .owner(testUser1)
                .isActive(true)
                .roomImageUrl("http://test.com/room.png")
                .roomThumbnailImageUrl("http://test.com/room_thumb.png")
                .build();
    }

    private ChatMessage createTestMessage() {
        return ChatMessage.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .sender(testUser1)
                .content("테스트 메시지")
                .type(MessageType.TEXT)
                .isDeleted(false)
                .build();
    }

    private ChatMessage createTestSystemMessage() {
        return ChatMessage.builder()
                .id(2L)
                .chatRoom(testChatRoom)
                .sender(null)
                .content("시스템 메시지")
                .type(MessageType.SYSTEM)
                .isDeleted(false)
                .build();
    }

    private ChatRoomUser createTestChatRoomUser(ChatRoom chatRoom, User user) {
        return ChatRoomUser.builder()
                .id(user.getId())
                .chatRoom(chatRoom)
                .user(user)
                .notificationEnabled(true)
                .build();
    }

    private ProfileImageResponseDto createTestProfileImage() {
        return ProfileImageResponseDto.builder()
                .userId(1L)
                .originalImageUrl("http://test.com/profile.png")
                .thumbnailImageUrl("http://test.com/profile_thumb.png")
                .isDefaultProfileImage(true)
                .build();
    }

    private ChatInvitation createTestInvitation() {
        return ChatInvitation.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .inviter(testUser1)
                .invitee(testUser2)
                .status(InvitationStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("채팅방 생성 테스트")
    class CreateChatRoomTest {

        @Test
        @DisplayName("성공: 그룹 채팅방 생성")
        void createChatRoom_Success() {
            // Given
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name("새로운 채팅방")
                    .userIds(Arrays.asList(2L))
                    .build();

            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));
            given(userRepository.findAllByIdWithProfileImage(anyList()))
                    .willReturn(Arrays.asList(testUser2));

            ChatRoom newChatRoom = ChatRoom.builder()
                    .id(1L)
                    .name("새로운 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(true)
                    .build();

            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willReturn(newChatRoom);
            given(chatRoomUserRepository.save(any(ChatRoomUser.class)))
                    .willReturn(testRoomUser);
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(anyLong()))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatRoomImageService.getDefaultImage(anyLong()))
                    .willReturn(ChatRoomImageResponseDto.builder()
                            .originalImageUrl("http://test.com/image.png")
                            .thumbnailImageUrl("http://test.com/thumb.png")
                            .isDefaultImage(true)
                            .build());
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willReturn(testSystemMessage);

            // When
            ChatRoomResponseDto result = chatService.createChatRoom(requestDto, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("새로운 채팅방");
            assertThat(result.getType()).isEqualTo(ChatRoomType.GROUP);
            assertThat(result.isOwner()).isTrue();

            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(chatRoomUserRepository, times(2)).save(any(ChatRoomUser.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void createChatRoom_UserNotFound() {
            // Given
            ChatRoomCreateRequestDto requestDto = ChatRoomCreateRequestDto.builder()
                    .name("새로운 채팅방")
                    .build();

            given(userRepository.findByIdAndIsDeletedFalse(999L))
                    .willReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatService.createChatRoom(requestDto, 999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("1:1 채팅방 생성 테스트")
    class CreateOneToOneChatRoomTest {

        @Test
        @DisplayName("성공: 새로운 1:1 채팅방 생성")
        void createOneToOneChatRoom_Success() {
            // Given
            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));
            given(userRepository.findByIdAndIsDeletedFalse(2L))
                    .willReturn(Optional.of(testUser2));
            given(chatRoomRepository.findOneToOneChatRoom(
                    eq(ChatRoomType.ONE_TO_ONE), eq(1L), eq(2L)))
                    .willReturn(Optional.empty());

            ChatRoom oneToOneChatRoom = ChatRoom.builder()
                    .id(2L)
                    .name("테스트유저1, 테스트유저2")
                    .type(ChatRoomType.ONE_TO_ONE)
                    .owner(testUser1)
                    .isActive(true)
                    .build();

            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willReturn(oneToOneChatRoom);
            given(chatRoomUserRepository.save(any(ChatRoomUser.class)))
                    .willReturn(testRoomUser);
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(anyLong()))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            ChatRoomResponseDto result = chatService.createOneToOneChatRoom(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("테스트유저1, 테스트유저2");
            assertThat(result.getType()).isEqualTo(ChatRoomType.ONE_TO_ONE);
            assertThat(result.isOwner()).isTrue();

            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(chatRoomUserRepository, times(2)).save(any(ChatRoomUser.class));
        }

        @Test
        @DisplayName("성공: 기존 1:1 채팅방 반환")
        void createOneToOneChatRoom_ExistingRoom() {
            // Given
            ChatRoom existingRoom = ChatRoom.builder()
                    .id(2L)
                    .name("기존 채팅방")
                    .type(ChatRoomType.ONE_TO_ONE)
                    .owner(testUser1)
                    .isActive(true)
                    .build();

            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));
            given(userRepository.findByIdAndIsDeletedFalse(2L))
                    .willReturn(Optional.of(testUser2));
            given(chatRoomRepository.findOneToOneChatRoom(
                    eq(ChatRoomType.ONE_TO_ONE), eq(1L), eq(2L)))
                    .willReturn(Optional.of(existingRoom));
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(anyLong()))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            ChatRoomResponseDto result = chatService.createOneToOneChatRoom(1L, 2L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getName()).isEqualTo("기존 채팅방");

            verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("실패: 자기 자신과의 채팅방 생성")
        void createOneToOneChatRoom_SelfChat() {
            // Given
            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));

            // When + Then
            assertThatThrownBy(() -> chatService.createOneToOneChatRoom(1L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("자기 자신과는 채팅할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("채팅방 목록 조회 테스트")
    class GetChatRoomsByUserIdTest {

        @Test
        @DisplayName("성공: 사용자의 채팅방 목록 조회")
        void getChatRoomsByUserId_Success() {
            // Given
            List<ChatRoom> chatRooms = Arrays.asList(testChatRoom);

            given(userRepository.existsById(1L)).willReturn(true);
            given(chatRoomRepository.findActiveByUserIdWithOwner(1L))
                    .willReturn(chatRooms);
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(anyLong()))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            List<ChatRoomResponseDto> result = chatService.getChatRoomsByUserId(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("테스트 채팅방");
            assertThat(result.get(0).isOwner()).isTrue();

            verify(chatRoomRepository).findActiveByUserIdWithOwner(1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void getChatRoomsByUserId_UserNotFound() {
            // Given
            given(userRepository.existsById(999L)).willReturn(false);

            // When + Then
            assertThatThrownBy(() -> chatService.getChatRoomsByUserId(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("메시지 전송 테스트")
    class SendMessageTest {

        @Test
        @DisplayName("성공: 텍스트 메시지 전송")
        void sendMessage_Success() {
            // Given
            ChatMessageRequestDto requestDto = ChatMessageRequestDto.builder()
                    .content("안녕하세요!")
                    .type(MessageType.TEXT)
                    .build();

            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willReturn(testMessage);

            // When
            ChatMessageResponseDto result = chatService.sendMessage(1L, requestDto, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo("테스트 메시지");
            assertThat(result.getType()).isEqualTo(MessageType.TEXT);
            assertThat(result.getSender().getId()).isEqualTo(1L);

            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방")
        void sendMessage_ChatRoomNotFound() {
            // Given
            ChatMessageRequestDto requestDto = ChatMessageRequestDto.builder()
                    .content("안녕하세요!")
                    .type(MessageType.TEXT)
                    .build();

            given(chatRoomRepository.findById(999L))
                    .willReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatService.sendMessage(999L, requestDto, 1L))
                    .isInstanceOf(ChatRoomNotFoundException.class);
        }

        @Test
        @DisplayName("실패: 비활성화된 채팅방")
        void sendMessage_InactiveChatRoom() {
            // Given
            ChatRoom inactiveChatRoom = ChatRoom.builder()
                    .id(1L)
                    .name("비활성 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(false)
                    .build();

            ChatMessageRequestDto requestDto = ChatMessageRequestDto.builder()
                    .content("안녕하세요!")
                    .type(MessageType.TEXT)
                    .build();

            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(inactiveChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When + Then
            assertThatThrownBy(() -> chatService.sendMessage(1L, requestDto, 1L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }

    @Nested
    @DisplayName("메시지 조회 테스트")
    class GetMessagesTest {

        @Test
        @DisplayName("성공: 메시지 목록 조회")
        void getMessages_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<ChatMessage> messages = Arrays.asList(testMessage);
            Page<ChatMessage> messagePage = new PageImpl<>(messages, pageable, 1);

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatMessageRepository.findByChatRoomIdWithSender(1L, pageable))
                    .willReturn(messagePage);
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));

            // When
            Page<ChatMessageResponseDto> result = chatService.getMessages(1L, 1L, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getContent()).isEqualTo("테스트 메시지");

            verify(chatMessageRepository).findByChatRoomIdWithSender(1L, pageable);
        }

        @Test
        @DisplayName("실패: 채팅방 접근 권한 없음")
        void getMessages_AccessDenied() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Collections.emptyList());

            // When + Then
            assertThatThrownBy(() -> chatService.getMessages(1L, 2L, pageable))
                    .isInstanceOf(ChatAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("메시지 삭제 테스트")
    class DeleteMessageTest {

        @Test
        @DisplayName("성공: 본인 메시지 삭제")
        void deleteMessage_Success() {
            // Given
            given(chatMessageRepository.findByIdAndSenderId(1L, 1L))
                    .willReturn(Optional.of(testMessage));
            given(userProfileImageService.getProfileImage(1L))
                    .willReturn(testProfileImage);

            // When
            chatService.deleteMessage(1L, 1L);

            // Then
            verify(messagingTemplate).convertAndSend(eq("/topic/chatroom/1"), any(ChatMessageResponseDto.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 메시지")
        void deleteMessage_MessageNotFound() {
            // Given
            given(chatMessageRepository.findByIdAndSenderId(999L, 1L))
                    .willReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatService.deleteMessage(999L, 1L))
                    .isInstanceOf(MessageNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("초대 링크 생성 테스트")
    class CreateInviteLinkTest {

        @Test
        @DisplayName("성공: 초대 링크 생성")
        void createInviteLink_Success() {
            // Given
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            ChatInviteResponseDto result = chatService.createInviteLink(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getInviteCode()).isNotNull();
            assertThat(result.getInviteLink()).startsWith("https://auratalk.com/invite/");
            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("실패: 비활성화된 채팅방")
        void createInviteLink_InactiveChatRoom() {
            // Given
            ChatRoom inactiveChatRoom = ChatRoom.builder()
                    .id(1L)
                    .name("비활성 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(false)
                    .build();

            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(inactiveChatRoom));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When + Then
            assertThatThrownBy(() -> chatService.createInviteLink(1L, 1L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }

    @Nested
    @DisplayName("초대 수락 테스트")
    class AcceptInviteTest {

        @Test
        @DisplayName("성공: 초대 링크로 채팅방 참여")
        void acceptInvite_Success() {
            // Given
            String inviteCode = "test-invite-code";

            ChatRoom inviteChatRoom = ChatRoom.builder()
                    .id(1L)
                    .name("테스트 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(true)
                    .inviteCode(inviteCode)
                    .inviteCodeExpiredAt(LocalDateTime.now().plusHours(24))
                    .build();

            given(chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode))
                    .willReturn(Optional.of(inviteChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(2L))
                    .willReturn(Optional.of(testUser2));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Collections.emptyList());
            given(chatRoomUserRepository.save(any(ChatRoomUser.class)))
                    .willReturn(testRoomUser2);
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willReturn(testSystemMessage);

            // When
            chatService.acceptInvite(inviteCode, 2L);

            // Then
            verify(chatRoomUserRepository).save(any(ChatRoomUser.class));
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("실패: 유효하지 않은 초대 코드")
        void acceptInvite_InvalidInviteCode() {
            // Given
            String inviteCode = "invalid-code";

            given(chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode))
                    .willReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatService.acceptInvite(inviteCode, 2L))
                    .isInstanceOf(ChatInvitationException.class);
        }

        @Test
        @DisplayName("실패: 만료된 초대 코드")
        void acceptInvite_ExpiredInviteCode() {
            // Given
            String inviteCode = "expired-code";

            ChatRoom expiredInviteChatRoom = ChatRoom.builder()
                    .id(1L)
                    .name("테스트 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(true)
                    .inviteCode(inviteCode)
                    .inviteCodeExpiredAt(LocalDateTime.now().minusHours(1))
                    .build();

            given(chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode))
                    .willReturn(Optional.of(expiredInviteChatRoom));

            // When + Then
            assertThatThrownBy(() -> chatService.acceptInvite(inviteCode, 2L))
                    .isInstanceOf(ChatInvitationException.class);
        }
    }

    @Nested
    @DisplayName("채팅방 나가기 테스트")
    class LeaveChatRoomTest {

        @Test
        @DisplayName("성공: 일반 멤버가 채팅방 나가기")
        void leaveChatRoom_Member_Success() {
            // Given
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(2L))
                    .willReturn(Optional.of(testUser2));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willReturn(testSystemMessage);

            // When
            chatService.leaveChatRoom(1L, 2L);

            // Then
            verify(chatRoomUserRepository).delete(testRoomUser);
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("실패: 채팅방 멤버가 아닌 사용자")
        void leaveChatRoom_NotMember() {
            // Given
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(2L))
                    .willReturn(Optional.of(testUser2));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Collections.emptyList());

            // When + Then
            assertThatThrownBy(() -> chatService.leaveChatRoom(1L, 2L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }

    @Nested
    @DisplayName("알림 설정 변경 테스트")
    class UpdateNotificationSettingsTest {

        @Test
        @DisplayName("성공: 알림 설정 변경")
        void updateNotificationSettings_Success() {
            // Given
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            chatService.updateNotificationSettings(1L, 1L, false);

            // Then
            verify(chatRoomUserRepository).findByChatRoomIdAndUserId(1L, 1L);
        }

        @Test
        @DisplayName("실패: 채팅방 멤버가 아닌 사용자")
        void updateNotificationSettings_NotMember() {
            // Given
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Collections.emptyList());

            // When + Then
            assertThatThrownBy(() -> chatService.updateNotificationSettings(1L, 2L, false))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }
}