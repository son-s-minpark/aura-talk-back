package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatInviteRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatMessageRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomCreateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatRoomUpdateRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.*;
import com.sonsminpark.auratalkback.domain.chat.entity.*;
import com.sonsminpark.auratalkback.domain.chat.exception.*;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatMessageRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.user.dto.response.ProfileImageResponseDto;
import com.sonsminpark.auratalkback.domain.user.entity.User;
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
        testUser1 = User.builder()
                .id(1L)
                .email("test1@example.com")
                .username("testuser1")
                .nickname("테스트유저1")
                .password("encodedPassword")
                .status(UserStatus.ONLINE)
                .isDeleted(false)
                .emailVerified(true)
                .build();

        testUser2 = User.builder()
                .id(2L)
                .email("test2@example.com")
                .username("testuser2")
                .nickname("테스트유저2")
                .password("encodedPassword")
                .status(UserStatus.ONLINE)
                .isDeleted(false)
                .emailVerified(true)
                .build();

        testUser3 = User.builder()
                .id(3L)
                .email("test3@example.com")
                .username("testuser3")
                .nickname("테스트유저3")
                .password("encodedPassword")
                .status(UserStatus.ONLINE)
                .isDeleted(false)
                .emailVerified(true)
                .build();

        testChatRoom = ChatRoom.builder()
                .id(1L)
                .name("테스트 채팅방")
                .type(ChatRoomType.GROUP)
                .owner(testUser1)
                .isActive(true)
                .roomImageUrl("http://test.com/room.png")
                .roomThumbnailImageUrl("http://test.com/room_thumb.png")
                .build();

        testMessage = ChatMessage.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .sender(testUser1)
                .content("테스트 메시지")
                .type(MessageType.TEXT)
                .isDeleted(false)
                .build();

        testSystemMessage = ChatMessage.builder()
                .id(2L)
                .chatRoom(testChatRoom)
                .sender(null)
                .content("시스템 메시지")
                .type(MessageType.SYSTEM)
                .isDeleted(false)
                .build();

        testRoomUser = ChatRoomUser.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .user(testUser1)
                .notificationEnabled(true)
                .build();

        testRoomUser2 = ChatRoomUser.builder()
                .id(2L)
                .chatRoom(testChatRoom)
                .user(testUser2)
                .notificationEnabled(true)
                .build();

        testProfileImage = ProfileImageResponseDto.builder()
                .userId(1L)
                .originalImageUrl("http://test.com/profile.png")
                .thumbnailImageUrl("http://test.com/profile_thumb.png")
                .isDefaultProfileImage(true)
                .build();

        testInvitation = ChatInvitation.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .inviter(testUser1)
                .invitee(testUser2)
                .status(InvitationStatus.PENDING)
                .build();

        ReflectionTestUtils.setField(chatService, "bucketName", "test-bucket");
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
    @DisplayName("채팅방 정보 조회 테스트")
    class GetChatRoomInfoTest {

        @Test
        @DisplayName("성공: 채팅방 정보 조회")
        void getChatRoomInfo_Success() {
            // Given
            given(chatRoomRepository.findByIdWithOwner(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            ChatRoomResponseDto result = chatService.getChatRoomInfo(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("테스트 채팅방");
            assertThat(result.isOwner()).isTrue();

            verify(chatRoomRepository).findByIdWithOwner(1L);
            verify(chatRoomRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 채팅방 접근 권한 없음")
        void getChatRoomInfo_AccessDenied() {
            // Given
            given(chatRoomRepository.findByIdWithOwner(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));

            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Collections.emptyList());

            // When + Then
            assertThatThrownBy(() -> chatService.getChatRoomInfo(1L, 2L))
                    .isInstanceOf(ChatAccessDeniedException.class);

            verify(chatRoomRepository).findByIdWithOwner(1L);
            verify(chatRoomRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("채팅방 정보 수정 테스트")
    class UpdateChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방 이름 수정")
        void updateChatRoom_NameUpdate_Success() {
            // Given
            ChatRoomUpdateRequestDto requestDto = ChatRoomUpdateRequestDto.builder()
                    .name("수정된 채팅방 이름")
                    .build();

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            ChatRoomResponseDto result = chatService.updateChatRoom(1L, requestDto, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(chatRoomRepository).findById(1L);
        }

        @Test
        @DisplayName("성공: 채팅방 이미지 수정")
        void updateChatRoom_ImageUpdate_Success() {
            // Given
            ChatRoomUpdateRequestDto requestDto = ChatRoomUpdateRequestDto.builder()
                    .roomImageUrl("http://test.com/new_image.png")
                    .roomThumbnailImageUrl("http://test.com/new_thumb.png")
                    .build();

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            ChatRoomResponseDto result = chatService.updateChatRoom(1L, requestDto, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(chatRoomRepository).findById(1L);
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 수정 시도")
        void updateChatRoom_NotOwner() {
            // Given
            ChatRoomUpdateRequestDto requestDto = ChatRoomUpdateRequestDto.builder()
                    .name("수정된 이름")
                    .build();

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));

            // When + Then
            assertThatThrownBy(() -> chatService.updateChatRoom(1L, requestDto, 2L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }

        @Test
        @DisplayName("실패: 비활성화된 채팅방")
        void updateChatRoom_InactiveChatRoom() {
            // Given
            ChatRoom inactiveChatRoom = ChatRoom.builder()
                    .id(1L)
                    .name("비활성 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(false)
                    .build();

            ChatRoomUpdateRequestDto requestDto = ChatRoomUpdateRequestDto.builder()
                    .name("수정된 이름")
                    .build();

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(inactiveChatRoom));

            // When + Then
            assertThatThrownBy(() -> chatService.updateChatRoom(1L, requestDto, 1L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }

    @Nested
    @DisplayName("채팅방 이미지 삭제 테스트")
    class DeleteRoomImageTest {

        @Test
        @DisplayName("성공: 채팅방 이미지 삭제")
        void deleteRoomImage_Success() {
            // Given
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(chatRoomImageService.deleteRoomImage(anyLong(), anyString(), anyString()))
                    .willReturn(ChatRoomImageResponseDto.builder()
                            .originalImageUrl("http://test.com/default.png")
                            .thumbnailImageUrl("http://test.com/default_thumb.png")
                            .isDefaultImage(true)
                            .build());
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            ChatRoomResponseDto result = chatService.deleteRoomImage(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            verify(chatRoomImageService).deleteRoomImage(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 삭제 시도")
        void deleteRoomImage_NotOwner() {
            // Given
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));

            // When + Then
            assertThatThrownBy(() -> chatService.deleteRoomImage(1L, 2L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }

    @Nested
    @DisplayName("채팅방 삭제 테스트")
    class DeleteChatRoomTest {

        @Test
        @DisplayName("성공: 채팅방 완전 삭제")
        void deleteChatRoom_Success() {
            // Given
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willReturn(testSystemMessage);

            // When
            chatService.deleteChatRoom(1L, 1L);

            // Then
            verify(chatRoomUserRepository).deleteAllByChatRoomId(1L);
            verify(chatMessageRepository).deleteAllByChatRoomId(1L);
            verify(chatRoomRepository).delete(testChatRoom);
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 삭제 시도")
        void deleteChatRoom_NotOwner() {
            // Given
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));

            // When + Then
            assertThatThrownBy(() -> chatService.deleteChatRoom(1L, 2L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }

    @Nested
    @DisplayName("사용자 차단 해제 테스트")
    class UnbanUserTest {

        @Test
        @DisplayName("성공: 사용자 차단 해제")
        void unbanUser_Success() {
            // Given
            Set<User> bannedUsers = new HashSet<>();
            bannedUsers.add(testUser2);

            ChatRoom chatRoomWithBannedUser = ChatRoom.builder()
                    .id(1L)
                    .name("테스트 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(true)
                    .bannedUsers(bannedUsers)
                    .build();

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoomWithBannedUser));
            given(userRepository.findByIdAndIsDeletedFalse(2L)).willReturn(Optional.of(testUser2));

            // When
            chatService.unbanUser(1L, 1L, 2L);

            // Then
            verify(chatRoomRepository).findById(1L);
            verify(userRepository).findByIdAndIsDeletedFalse(2L);
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 차단 해제 시도")
        void unbanUser_NotOwner() {
            // Given
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(2L)).willReturn(Optional.of(testUser2));

            // When + Then
            assertThatThrownBy(() -> chatService.unbanUser(1L, 2L, 2L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }

        @Test
        @DisplayName("실패: 자기 자신의 차단 해제 시도")
        void unbanUser_SelfUnban() {
            // Given
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.of(testUser1));

            // When + Then
            assertThatThrownBy(() -> chatService.unbanUser(1L, 1L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("자기 자신의 강퇴를 해제할 수 없습니다.");
        }

        @Test
        @DisplayName("실패: 강퇴되지 않은 사용자")
        void unbanUser_NotBannedUser() {
            // Given
            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(2L)).willReturn(Optional.of(testUser2));

            // When + Then
            assertThatThrownBy(() -> chatService.unbanUser(1L, 1L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("강퇴되지 않은 사용자입니다.");
        }
    }

    @Nested
    @DisplayName("차단된 사용자 목록 조회 테스트")
    class GetBannedUsersTest {

        @Test
        @DisplayName("성공: 차단된 사용자 목록 조회")
        void getBannedUsers_Success() {
            // Given
            Set<User> bannedUsers = new HashSet<>();
            bannedUsers.add(testUser2);

            ChatRoom chatRoomWithBannedUsers = ChatRoom.builder()
                    .id(1L)
                    .name("테스트 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(true)
                    .bannedUsers(bannedUsers)
                    .build();

            given(chatRoomRepository.findByIdWithBannedUsers(1L))
                    .willReturn(Optional.of(chatRoomWithBannedUsers));

            // When
            List<ChatUserResponseDto> result = chatService.getBannedUsers(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(2L);

            verify(chatRoomRepository).findByIdWithBannedUsers(1L);
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 조회 시도")
        void getBannedUsers_NotOwner() {
            // Given
            given(chatRoomRepository.findByIdWithBannedUsers(1L))
                    .willReturn(Optional.of(testChatRoom));

            // When + Then
            assertThatThrownBy(() -> chatService.getBannedUsers(1L, 2L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }

    @Nested
    @DisplayName("채팅방 검색 테스트")
    class SearchChatRoomsTest {

        @Test
        @DisplayName("성공: 채팅방 검색")
        void searchChatRooms_Success() {
            // Given
            List<ChatRoom> searchResults = Arrays.asList(testChatRoom);

            given(userRepository.existsById(1L)).willReturn(true);
            given(chatRoomRepository.searchByNameAndUserId("테스트", 1L))
                    .willReturn(searchResults);
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(1L))
                    .willReturn(Arrays.asList(testRoomUser));

            // When
            List<ChatRoomResponseDto> result = chatService.searchChatRooms("테스트", 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("테스트 채팅방");

            verify(chatRoomRepository).searchByNameAndUserId("테스트", 1L);
        }

        @Test
        @DisplayName("성공: 빈 키워드로 검색")
        void searchChatRooms_EmptyKeyword() {
            // Given
            given(userRepository.existsById(1L)).willReturn(true);

            // When
            List<ChatRoomResponseDto> result = chatService.searchChatRooms("", 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(chatRoomRepository, never()).searchByNameAndUserId(anyString(), anyLong());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void searchChatRooms_UserNotFound() {
            // Given
            given(userRepository.existsById(999L)).willReturn(false);

            // When + Then
            assertThatThrownBy(() -> chatService.searchChatRooms("테스트", 999L))
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
            verify(messagingTemplate).convertAndSend(eq("/topic/chatroom/1"), any(ChatMessageResponseDto.class));
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
        @DisplayName("실패: 채팅방 멤버가 아닌 사용자")
        void sendMessage_NotMember() {
            // Given
            ChatMessageRequestDto requestDto = ChatMessageRequestDto.builder()
                    .content("안녕하세요!")
                    .type(MessageType.TEXT)
                    .build();

            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(2L))
                    .willReturn(Optional.of(testUser2));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Collections.emptyList());

            // When + Then
            assertThatThrownBy(() -> chatService.sendMessage(1L, requestDto, 2L))
                    .isInstanceOf(ChatAccessDeniedException.class);
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

        @Test
        @DisplayName("실패: 다른 사용자의 메시지 삭제 시도")
        void deleteMessage_NotOwner() {
            // Given
            given(chatMessageRepository.findByIdAndSenderId(1L, 2L))
                    .willReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatService.deleteMessage(1L, 2L))
                    .isInstanceOf(MessageNotFoundException.class);
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
        @DisplayName("성공: 방장이 채팅방 나가기 (채팅방 비활성화)")
        void leaveChatRoom_Owner_Success() {
            // Given
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatMessageRepository.save(any(ChatMessage.class)))
                    .willReturn(testSystemMessage);

            // When
            chatService.leaveChatRoom(1L, 1L);

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
    @DisplayName("친구 초대 테스트")
    class SendInviteToFriendTest {

        @Test
        @DisplayName("성공: 친구에게 초대 링크 전송")
        void sendInviteToFriend_Success() {
            // Given
            ChatInviteRequestDto requestDto = ChatInviteRequestDto.builder()
                    .userId(2L)
                    .build();

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.of(testUser1));
            given(userRepository.findByIdAndIsDeletedFalse(2L)).willReturn(Optional.of(testUser2));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Collections.emptyList());

            // When
            ChatInviteResponseDto result = chatService.sendInviteToFriend(1L, requestDto, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getInviteCode()).isNotNull();
            assertThat(result.getInviteLink()).startsWith("https://auratalk.com/invite/");

            verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패: 이미 채팅방에 참여중인 사용자 초대")
        void sendInviteToFriend_AlreadyMember() {
            // Given
            ChatInviteRequestDto requestDto = ChatInviteRequestDto.builder()
                    .userId(2L)
                    .build();

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.of(testUser1));
            given(userRepository.findByIdAndIsDeletedFalse(2L)).willReturn(Optional.of(testUser2));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Arrays.asList(testRoomUser2));

            // When + Then
            assertThatThrownBy(() -> chatService.sendInviteToFriend(1L, requestDto, 1L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }

        @Test
        @DisplayName("실패: 강퇴된 사용자 초대")
        void sendInviteToFriend_BannedUser() {
            // Given
            Set<User> bannedUsers = new HashSet<>();
            bannedUsers.add(testUser2);

            ChatRoom chatRoomWithBannedUser = ChatRoom.builder()
                    .id(1L)
                    .name("테스트 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(true)
                    .bannedUsers(bannedUsers)
                    .build();

            ChatInviteRequestDto requestDto = ChatInviteRequestDto.builder()
                    .userId(2L)
                    .build();

            given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoomWithBannedUser));
            given(userRepository.findByIdAndIsDeletedFalse(1L)).willReturn(Optional.of(testUser1));
            given(userRepository.findByIdAndIsDeletedFalse(2L)).willReturn(Optional.of(testUser2));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L))
                    .willReturn(Arrays.asList(testRoomUser));
            given(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L))
                    .willReturn(Collections.emptyList());

            // When + Then
            assertThatThrownBy(() -> chatService.sendInviteToFriend(1L, requestDto, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("강퇴된 사용자는 초대할 수 없습니다.");
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

        @Test
        @DisplayName("실패: 강퇴된 사용자의 참여 시도")
        void acceptInvite_BannedUser() {
            // Given
            String inviteCode = "test-invite-code";
            Set<User> bannedUsers = new HashSet<>();
            bannedUsers.add(testUser2);

            ChatRoom chatRoomWithBannedUser = ChatRoom.builder()
                    .id(1L)
                    .name("테스트 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser1)
                    .isActive(true)
                    .inviteCode(inviteCode)
                    .inviteCodeExpiredAt(LocalDateTime.now().plusHours(24))
                    .bannedUsers(bannedUsers)
                    .build();

            given(chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode))
                    .willReturn(Optional.of(chatRoomWithBannedUser));

            // When + Then
            assertThatThrownBy(() -> chatService.acceptInvite(inviteCode, 2L))
                    .isInstanceOf(ChatRoomBannedException.class);
        }

        @Test
        @DisplayName("실패: 이미 채팅방에 참여중인 사용자")
        void acceptInvite_AlreadyMember() {
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
                    .willReturn(Arrays.asList(testRoomUser2));

            // When + Then
            assertThatThrownBy(() -> chatService.acceptInvite(inviteCode, 2L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }
    }

    @Nested
    @DisplayName("사용자 강퇴 테스트")
    class KickUserTest {

        @Test
        @DisplayName("성공: 사용자 강퇴")
        void kickUser_Success() {
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
            chatService.kickUser(1L, 1L, 2L);

            // Then
            verify(chatRoomUserRepository).delete(testRoomUser);
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(messagingTemplate).convertAndSendToUser(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패: 방장이 아닌 사용자의 강퇴 시도")
        void kickUser_NotOwner() {
            // Given
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));

            // When + Then
            assertThatThrownBy(() -> chatService.kickUser(1L, 2L, 1L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }

        @Test
        @DisplayName("실패: 자기 자신 강퇴 시도")
        void kickUser_SelfKick() {
            // Given
            given(chatRoomRepository.findById(1L))
                    .willReturn(Optional.of(testChatRoom));
            given(userRepository.findByIdAndIsDeletedFalse(1L))
                    .willReturn(Optional.of(testUser1));

            // When + Then
            assertThatThrownBy(() -> chatService.kickUser(1L, 1L, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("자기 자신을 강퇴할 수 없습니다.");
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