package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.RandomChatStartRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.RandomChatMatchResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoom;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomType;
import com.sonsminpark.auratalkback.domain.chat.entity.ChatRoomUser;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomRepository;
import com.sonsminpark.auratalkback.domain.chat.repository.ChatRoomUserRepository;
import com.sonsminpark.auratalkback.domain.friend.entity.FriendBlock;
import com.sonsminpark.auratalkback.domain.friend.repository.FriendBlockRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "cloud.aws.s3.bucket=test-bucket"
})
@DisplayName("RandomChatService 테스트")
class RandomChatServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomUserRepository chatRoomUserRepository;

    @Mock
    private FriendBlockRepository friendBlockRepository;

    @Mock
    private UserProfileImageService userProfileImageService;

    @InjectMocks
    private RandomChatServiceImpl randomChatService;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private User disabledUser;
    private ChatRoom testChatRoom;

    @BeforeEach
    void setUp() {
        testUser1 = createTestUser(1L, "user1@example.com", "user1", "사용자1", true, List.of("게임", "영화"));
        testUser2 = createTestUser(2L, "user2@example.com", "user2", "사용자2", true, List.of("게임", "음악"));
        testUser3 = createTestUser(3L, "user3@example.com", "user3", "사용자3", true, List.of("독서", "운동"));
        disabledUser = createTestUser(4L, "disabled@example.com", "disabled", "비활성사용자", false, List.of("게임"));

        testChatRoom = ChatRoom.builder()
                .id(1L)
                .name("랜덤 채팅 - 사용자1 & 사용자2")
                .type(ChatRoomType.RANDOM)
                .owner(testUser1)
                .isActive(true)
                .build();
    }

    private User createTestUser(Long id, String email, String username, String nickname,
                                boolean randomChatEnabled, List<String> interests) {
        User user = User.builder()
                .id(id)
                .email(email)
                .username(username)
                .nickname(nickname)
                .password("password")
                .status(UserStatus.ONLINE)
                .isDeleted(false)
                .randomChatEnabled(randomChatEnabled)
                .build();

        // interests 설정
        ReflectionTestUtils.setField(user, "interests", interests);

        // UserProfileImage 설정 (testUser2만)
        if (id.equals(2L)) {
            UserProfileImage profileImage = UserProfileImage.builder()
                    .user(user)
                    .originalImageUrl("http://test.com/profile.png")
                    .thumbnailImageUrl("http://test.com/profile_thumb.png")
                    .isDefaultProfileImage(false)
                    .build();
            ReflectionTestUtils.setField(user, "userProfileImage", profileImage);
        }

        return user;
    }

    @Nested
    @DisplayName("랜덤 채팅 시작 테스트")
    class StartRandomChatTest {

        @Test
        @DisplayName("성공: 관심사 기반 매칭 성공")
        void startRandomChat_SuccessfulMatch() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(testUser1));
            given(friendBlockRepository.findBlockedUsers(1L))
                    .willReturn(List.of());
            given(userRepository.findActiveUsersByInterest("게임"))
                    .willReturn(List.of(testUser1, testUser2));
            given(friendBlockRepository.findByBlockerAndBlocked(testUser2, testUser1))
                    .willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willReturn(testChatRoom);
            given(chatRoomUserRepository.save(any(ChatRoomUser.class)))
                    .willReturn(mock(ChatRoomUser.class));
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(anyLong()))
                    .willReturn(List.of(
                            createMockChatRoomUser(testUser1),
                            createMockChatRoomUser(testUser2)
                    ));

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isTrue();
            assertThat(result.isWaiting()).isFalse();
            assertThat(result.getChatRoom()).isNotNull();
            assertThat(result.getMatchedUser()).isNotNull();
            assertThat(result.getMatchedUser().getId()).isEqualTo(2L);
            assertThat(result.getMessage()).isEqualTo("매칭이 완료되었습니다!");

            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(chatRoomUserRepository, times(2)).save(any(ChatRoomUser.class));
        }

        @Test
        @DisplayName("성공: 사용자 관심사로 매칭 (요청에 관심사 없음)")
        void startRandomChat_UseUserInterests() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of())
                    .build();

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(testUser1));
            given(friendBlockRepository.findBlockedUsers(1L))
                    .willReturn(List.of());
            given(userRepository.findActiveUsersByInterest("게임"))
                    .willReturn(List.of(testUser1, testUser2));
            given(userRepository.findActiveUsersByInterest("영화"))
                    .willReturn(List.of(testUser1));
            given(friendBlockRepository.findByBlockerAndBlocked(testUser2, testUser1))
                    .willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willReturn(testChatRoom);
            given(chatRoomUserRepository.save(any(ChatRoomUser.class)))
                    .willReturn(mock(ChatRoomUser.class));
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(anyLong()))
                    .willReturn(List.of(
                            createMockChatRoomUser(testUser1),
                            createMockChatRoomUser(testUser2)
                    ));

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isTrue();

            verify(userRepository).findActiveUsersByInterest("게임");
            verify(userRepository).findActiveUsersByInterest("영화");
        }

        @Test
        @DisplayName("실패: 랜덤 채팅 비활성화된 사용자")
        void startRandomChat_RandomChatDisabled() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            User disabledChatUser = createTestUser(1L, "disabled@example.com", "disabled", "비활성사용자", false, List.of("게임"));

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(disabledChatUser));

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isFalse();
            assertThat(result.isWaiting()).isFalse();
            assertThat(result.getMessage()).isEqualTo("랜덤 채팅이 비활성화되어 있습니다. 설정에서 활성화해주세요.");

            verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("실패: 관심사가 없는 사용자")
        void startRandomChat_NoInterests() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of())
                    .build();

            User userWithoutInterests = createTestUser(1L, "nointerests@example.com", "nointerests", "관심사없음", true, null);

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(userWithoutInterests));

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isEqualTo("관심사를 설정해주세요.");
        }

        @Test
        @DisplayName("실패: 매칭 가능한 사용자 없음")
        void startRandomChat_NoMatchingUsers() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("희귀한취미"))
                    .build();

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(testUser1));
            given(friendBlockRepository.findBlockedUsers(1L))
                    .willReturn(List.of());
            given(userRepository.findActiveUsersByInterest("희귀한취미"))
                    .willReturn(List.of(testUser1)); // 본인만 반환

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isEqualTo("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.");

            verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        }

        @Test
        @DisplayName("실패: 차단된 사용자만 있는 경우")
        void startRandomChat_OnlyBlockedUsers() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(testUser1));
            given(friendBlockRepository.findBlockedUsers(1L))
                    .willReturn(List.of(testUser2)); // testUser2를 차단함
            given(userRepository.findActiveUsersByInterest("게임"))
                    .willReturn(List.of(testUser1, testUser2));

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isEqualTo("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.");
        }

        @Test
        @DisplayName("실패: 사용자를 차단한 사용자만 있는 경우")
        void startRandomChat_BlockedByOtherUser() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(testUser1));
            given(friendBlockRepository.findBlockedUsers(1L))
                    .willReturn(List.of());
            given(userRepository.findActiveUsersByInterest("게임"))
                    .willReturn(List.of(testUser1, testUser2));
            given(friendBlockRepository.findByBlockerAndBlocked(testUser2, testUser1))
                    .willReturn(Optional.of(mock(FriendBlock.class))); // testUser2가 testUser1을 차단함

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isEqualTo("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.");
        }

        @Test
        @DisplayName("실패: 랜덤 채팅 비활성화된 사용자만 있는 경우")
        void startRandomChat_OnlyDisabledUsers() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(testUser1));
            given(friendBlockRepository.findBlockedUsers(1L))
                    .willReturn(List.of());
            given(userRepository.findActiveUsersByInterest("게임"))
                    .willReturn(List.of(testUser1, disabledUser));
            given(friendBlockRepository.findByBlockerAndBlocked(disabledUser, testUser1))
                    .willReturn(Optional.empty());

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isEqualTo("현재 매칭 가능한 사용자가 없습니다. 잠시 후 다시 시도해주세요.");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void startRandomChat_UserNotFound() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(userRepository.findByIdWithProfileImage(999L))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> randomChatService.startRandomChat(999L, requestDto))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("실패: 매칭 중 예외 발생")
        void startRandomChat_ExceptionDuringMatching() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of("게임"))
                    .build();

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(testUser1));
            given(friendBlockRepository.findBlockedUsers(1L))
                    .willThrow(new RuntimeException("Database error"));

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isEqualTo("매칭 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }

        @Test
        @DisplayName("성공: 빈 관심사 리스트인 사용자")
        void startRandomChat_EmptyInterestsList() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(List.of())
                    .build();

            User userWithEmptyInterests = createTestUser(1L, "emptyinterests@example.com", "emptyinterests", "빈관심사", true, List.of());

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(userWithEmptyInterests));

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isFalse();
            assertThat(result.getMessage()).isEqualTo("관심사를 설정해주세요.");
        }

        @Test
        @DisplayName("성공: null 관심사로 매칭")
        void startRandomChat_NullInterests() {
            // Given
            RandomChatStartRequestDto requestDto = RandomChatStartRequestDto.builder()
                    .interests(null)
                    .build();

            given(userRepository.findByIdWithProfileImage(1L))
                    .willReturn(Optional.of(testUser1));
            given(friendBlockRepository.findBlockedUsers(1L))
                    .willReturn(List.of());
            given(userRepository.findActiveUsersByInterest("게임"))
                    .willReturn(List.of(testUser1, testUser2));
            given(userRepository.findActiveUsersByInterest("영화"))
                    .willReturn(List.of(testUser1));
            given(friendBlockRepository.findByBlockerAndBlocked(testUser2, testUser1))
                    .willReturn(Optional.empty());
            given(chatRoomRepository.save(any(ChatRoom.class)))
                    .willReturn(testChatRoom);
            given(chatRoomUserRepository.save(any(ChatRoomUser.class)))
                    .willReturn(mock(ChatRoomUser.class));
            given(chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(anyLong()))
                    .willReturn(List.of(
                            createMockChatRoomUser(testUser1),
                            createMockChatRoomUser(testUser2)
                    ));

            // When
            RandomChatMatchResponseDto result = randomChatService.startRandomChat(1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isMatched()).isTrue();

            verify(userRepository).findActiveUsersByInterest("게임");
            verify(userRepository).findActiveUsersByInterest("영화");
        }
    }

    private ChatRoomUser createMockChatRoomUser(User user) {
        ChatRoomUser mockRoomUser = mock(ChatRoomUser.class);
        given(mockRoomUser.getUser()).willReturn(user);
        return mockRoomUser;
    }
}