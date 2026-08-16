package com.sonsminpark.auratalkback.domain.chat.repository;

import com.sonsminpark.auratalkback.domain.chat.entity.*;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserProfileImage;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(locations = "classpath:application-test.yml")
@ActiveProfiles("test")
@DisplayName("Chat Repository 테스트")
class ChatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomUserRepository chatRoomUserRepository;

    @Autowired
    private ChatFileRepository chatFileRepository;

    @Autowired
    private ChatInvitationRepository chatInvitationRepository;

    private User testUser1;
    private User testUser2;
    private User testUser3;
    private ChatRoom testChatRoom1;
    private ChatRoom testChatRoom2;
    private ChatRoom oneToOneChatRoom;
    private ChatMessage testMessage;
    private ChatFile testFile;
    private ChatInvitation testInvitation;

    @BeforeEach
    void setUp() {
        // 테스트 사용자들 생성
        testUser1 = createAndSaveUser("test1@example.com", "testuser1", "테스트유저1");
        testUser2 = createAndSaveUser("test2@example.com", "testuser2", "테스트유저2");
        testUser3 = createAndSaveUser("test3@example.com", "testuser3", "테스트유저3");

        // 테스트 채팅방들 생성
        testChatRoom1 = createAndSaveChatRoom("테스트 채팅방 1", ChatRoomType.GROUP, testUser1);
        testChatRoom2 = createAndSaveChatRoom("테스트 채팅방 2", ChatRoomType.GROUP, testUser2);
        oneToOneChatRoom = createAndSaveChatRoom("1:1 채팅방", ChatRoomType.ONE_TO_ONE, testUser1);

        // 채팅방 사용자 관계 생성
        createAndSaveChatRoomUser(testChatRoom1, testUser1);
        createAndSaveChatRoomUser(testChatRoom1, testUser2);
        createAndSaveChatRoomUser(testChatRoom2, testUser2);
        createAndSaveChatRoomUser(oneToOneChatRoom, testUser1);
        createAndSaveChatRoomUser(oneToOneChatRoom, testUser2);

        // 테스트 메시지 생성
        testMessage = createAndSaveMessage(testChatRoom1, testUser1, "테스트 메시지", MessageType.TEXT);

        // 테스트 파일 생성
        testFile = createAndSaveFile(testChatRoom1, testUser1, "test.jpg");

        // 테스트 초대 생성
        testInvitation = createAndSaveInvitation(testChatRoom1, testUser1, testUser3);

        entityManager.flush();
        entityManager.clear();
    }

    private User createAndSaveUser(String email, String username, String nickname) {
        User user = User.builder()
                .email(email)
                .username(username)
                .nickname(nickname)
                .password("encodedPassword")
                .status(UserStatus.ONLINE)
                .isDeleted(false)
                .emailVerified(true)
                .randomChatEnabled(true)
                .build();

        User savedUser = entityManager.persistAndFlush(user);

        UserProfileImage profileImage = UserProfileImage.builder()
                .user(savedUser)
                .originalImageUrl("http://test.com/" + username + ".png")
                .thumbnailImageUrl("http://test.com/" + username + "_thumb.png")
                .isDefaultProfileImage(false)
                .build();

        entityManager.persistAndFlush(profileImage);

        entityManager.refresh(savedUser);
        return savedUser;
    }

    private ChatRoom createAndSaveChatRoom(String name, ChatRoomType type, User owner) {
        ChatRoom chatRoom = ChatRoom.builder()
                .name(name)
                .type(type)
                .owner(owner)
                .isActive(true)
                .lastMessageAt(LocalDateTime.now().minusHours(1))
                .build();
        return entityManager.persistAndFlush(chatRoom);
    }

    private ChatRoomUser createAndSaveChatRoomUser(ChatRoom chatRoom, User user) {
        ChatRoomUser roomUser = ChatRoomUser.builder()
                .chatRoom(chatRoom)
                .user(user)
                .notificationEnabled(true)
                .build();
        return entityManager.persistAndFlush(roomUser);
    }

    private ChatMessage createAndSaveMessage(ChatRoom chatRoom, User sender, String content, MessageType type) {
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(content)
                .type(type)
                .isDeleted(false)
                .build();
        return entityManager.persistAndFlush(message);
    }

    private ChatFile createAndSaveFile(ChatRoom chatRoom, User uploader, String fileName) {
        ChatFile file = ChatFile.builder()
                .chatRoom(chatRoom)
                .uploader(uploader)
                .originalFileName(fileName)
                .s3Key("chat-files/" + fileName)
                .s3Url("https://test.s3.amazonaws.com/chat-files/" + fileName)
                .fileExtension("jpg")
                .mimeType("image/jpeg")
                .fileSize(1024L)
                .isDeleted(false)
                .build();
        return entityManager.persistAndFlush(file);
    }

    private ChatInvitation createAndSaveInvitation(ChatRoom chatRoom, User inviter, User invitee) {
        ChatInvitation invitation = ChatInvitation.builder()
                .chatRoom(chatRoom)
                .inviter(inviter)
                .invitee(invitee)
                .status(InvitationStatus.PENDING)
                .build();
        return entityManager.persistAndFlush(invitation);
    }

    @Nested
    @DisplayName("ChatRoomRepository 테스트")
    class ChatRoomRepositoryTest {

        @Test
        @DisplayName("사용자별 활성 채팅방 조회 - 소유자 정보 포함")
        void findActiveByUserIdWithOwner_Success() {
            // When
            List<ChatRoom> result = chatRoomRepository.findActiveByUserIdWithOwner(testUser1.getId());

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOwner()).isNotNull();
        }

        @Test
        @DisplayName("사용자별 활성 채팅방 페이징 조회")
        void findActiveByUserIdWithOwnerPaging_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<ChatRoom> result = chatRoomRepository.findActiveByUserIdWithOwnerPaging(testUser1.getId(), pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("1:1 채팅방 조회")
        void findOneToOneChatRoom_Success() {
            // When
            Optional<ChatRoom> result = chatRoomRepository.findOneToOneChatRoom(
                    ChatRoomType.ONE_TO_ONE, testUser1.getId(), testUser2.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(ChatRoomType.ONE_TO_ONE);
        }

        @Test
        @DisplayName("1:1 채팅방 조회 - 존재하지 않는 경우")
        void findOneToOneChatRoom_NotFound() {
            // When
            Optional<ChatRoom> result = chatRoomRepository.findOneToOneChatRoom(
                    ChatRoomType.ONE_TO_ONE, testUser1.getId(), testUser3.getId());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("초대 코드로 활성 채팅방 조회")
        void findByInviteCodeAndIsActiveTrue_Success() {
            // Given
            String inviteCode = "test-invite-code";
            testChatRoom1.generateInviteCode(inviteCode, LocalDateTime.now().plusHours(24));
            entityManager.merge(testChatRoom1);
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<ChatRoom> result = chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getInviteCode()).isEqualTo(inviteCode);
        }

        @Test
        @DisplayName("사용자가 채팅방에 속해있는지 확인")
        void isUserInChatRoom_True() {
            // When
            boolean result = chatRoomRepository.isUserInChatRoom(testChatRoom1.getId(), testUser1.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("사용자가 채팅방에 속해있지 않음 확인")
        void isUserInChatRoom_False() {
            // When
            boolean result = chatRoomRepository.isUserInChatRoom(testChatRoom1.getId(), testUser3.getId());

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("채팅방 이름으로 검색")
        void searchByName_Success() {
            // When
            List<ChatRoom> result = chatRoomRepository.searchByName("테스트");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(room -> room.getName().contains("테스트"));
        }

        @Test
        @DisplayName("채팅방 이름과 사용자별 검색")
        void searchByNameAndUserId_Success() {
            // When
            List<ChatRoom> result = chatRoomRepository.searchByNameAndUserId("테스트", testUser1.getId());

            // Then
            assertThat(result).hasSize(2); // 채팅방 이름 + 참여자 닉네임이므로 2개
            assertThat(result).anyMatch(room -> room.getName().contains("테스트"));
        }

        @Test
        @DisplayName("ID로 채팅방 조회 - 소유자 정보 포함")
        void findByIdWithOwner_Success() {
            // When
            Optional<ChatRoom> result = chatRoomRepository.findByIdWithOwner(testChatRoom1.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getOwner()).isNotNull();
        }

        @Test
        @DisplayName("ID로 채팅방 조회 - 차단된 사용자 정보 포함")
        void findByIdWithBannedUsers_Success() {
            // Given
            testChatRoom1.banUser(testUser3);
            entityManager.merge(testChatRoom1);
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<ChatRoom> result = chatRoomRepository.findByIdWithBannedUsers(testChatRoom1.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getBannedUsers()).hasSize(1);
            assertThat(result.get().getBannedUsers().iterator().next().getId()).isEqualTo(testUser3.getId());
        }
    }

    @Nested
    @DisplayName("ChatMessageRepository 테스트")
    class ChatMessageRepositoryTest {

        @Test
        @DisplayName("채팅방별 메시지 조회 - 발신자 정보 포함")
        void findByChatRoomIdWithSender_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<ChatMessage> result = chatMessageRepository.findByChatRoomIdWithSender(testChatRoom1.getId(), pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getSender()).isNotNull();
        }

        @Test
        @DisplayName("메시지 ID와 발신자 ID로 조회")
        void findByIdAndSenderId_Success() {
            // When
            Optional<ChatMessage> result = chatMessageRepository.findByIdAndSenderId(testMessage.getId(), testUser1.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(testMessage.getId());
        }

        @Test
        @DisplayName("메시지 ID와 발신자 ID로 조회 - 다른 사용자")
        void findByIdAndSenderId_DifferentUser() {
            // When
            Optional<ChatMessage> result = chatMessageRepository.findByIdAndSenderId(testMessage.getId(), testUser2.getId());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("채팅방별 삭제되지 않은 메시지 개수 조회")
        void countByChatRoomIdAndIsDeletedFalse_Success() {
            // When
            long count = chatMessageRepository.countByChatRoomIdAndIsDeletedFalse(testChatRoom1.getId());

            // Then
            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("ChatRoomUserRepository 테스트")
    class ChatRoomUserRepositoryTest {

        @Test
        @DisplayName("채팅방과 사용자별 관계 조회")
        void findByChatRoomIdAndUserId_Success() {
            // When
            List<ChatRoomUser> result = chatRoomUserRepository.findByChatRoomIdAndUserId(testChatRoom1.getId(), testUser1.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUser().getId()).isEqualTo(testUser1.getId());
        }

        @Test
        @DisplayName("사용자 설정 조회")
        void findUserSettings_Success() {
            // When
            Optional<ChatRoomUser> result = chatRoomUserRepository.findUserSettings(testChatRoom1.getId(), testUser1.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().isNotificationEnabled()).isTrue();
        }

        @Test
        @DisplayName("채팅방별 모든 사용자 조회 - 사용자와 프로필 정보 포함")
        void findAllByChatRoomIdWithUserAndProfile_Success() {
            // When
            List<ChatRoomUser> result = chatRoomUserRepository.findAllByChatRoomIdWithUserAndProfile(testChatRoom1.getId());

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(roomUser -> roomUser.getUser() != null);
        }

        @Test
        @DisplayName("채팅방별 사용자 수 조회")
        void countByChatRoomId_Success() {
            // When
            long count = chatRoomUserRepository.countByChatRoomId(testChatRoom1.getId());

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("ChatFileRepository 테스트")
    class ChatFileRepositoryTest {

        @Test
        @DisplayName("채팅방별 파일 조회 - 업로더 정보 포함")
        void findByChatRoomIdWithUploader_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<ChatFile> result = chatFileRepository.findByChatRoomIdWithUploader(testChatRoom1.getId(), pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUploader()).isNotNull();
        }

        @Test
        @DisplayName("파일 ID와 업로더 ID로 조회")
        void findByIdAndUploaderIdAndIsDeletedFalse_Success() {
            // When
            Optional<ChatFile> result = chatFileRepository.findByIdAndUploaderIdAndIsDeletedFalse(
                    testFile.getId(), testUser1.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUploader().getId()).isEqualTo(testUser1.getId());
        }

        @Test
        @DisplayName("파일 ID와 업로더 ID로 조회 - 다른 사용자")
        void findByIdAndUploaderIdAndIsDeletedFalse_DifferentUser() {
            // When
            Optional<ChatFile> result = chatFileRepository.findByIdAndUploaderIdAndIsDeletedFalse(
                    testFile.getId(), testUser2.getId());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("파일 ID로 조회 - 삭제되지 않은 파일만")
        void findByIdAndIsDeletedFalse_Success() {
            // When
            Optional<ChatFile> result = chatFileRepository.findByIdAndIsDeletedFalse(testFile.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().isDeleted()).isFalse();
        }

        @Test
        @DisplayName("채팅방별 이미지 파일 조회")
        void findImagesByChatRoomId_Success() {
            // When
            List<ChatFile> result = chatFileRepository.findImagesByChatRoomId(testChatRoom1.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMimeType()).startsWith("image/");
        }

        @Test
        @DisplayName("채팅방별 비디오 파일 조회")
        void findVideosByChatRoomId_EmptyResult() {
            // When
            List<ChatFile> result = chatFileRepository.findVideosByChatRoomId(testChatRoom1.getId());

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("ChatInvitationRepository 테스트")
    class ChatInvitationRepositoryTest {

        @Test
        @DisplayName("피초대자와 상태별 초대 조회")
        void findByInviteeIdAndStatus_Success() {
            // When
            List<ChatInvitation> result = chatInvitationRepository.findByInviteeIdAndStatus(
                    testUser3.getId(), InvitationStatus.PENDING);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(InvitationStatus.PENDING);
        }

        @Test
        @DisplayName("채팅방, 피초대자, 상태별 초대 조회")
        void findByChatRoomIdAndInviteeIdAndStatus_Success() {
            // When
            Optional<ChatInvitation> result = chatInvitationRepository.findByChatRoomIdAndInviteeIdAndStatus(
                    testChatRoom1.getId(), testUser3.getId(), InvitationStatus.PENDING);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getChatRoom().getId()).isEqualTo(testChatRoom1.getId());
        }

        @Test
        @DisplayName("초대자별 초대 조회")
        void findByInviterId_Success() {
            // When
            List<ChatInvitation> result = chatInvitationRepository.findByInviterId(testUser1.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getInviter().getId()).isEqualTo(testUser1.getId());
        }
    }

    @Nested
    @DisplayName("경계값 및 예외 상황 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("존재하지 않는 사용자로 채팅방 조회")
        void findChatRoomsWithNonExistentUser() {
            // When
            List<ChatRoom> result = chatRoomRepository.findActiveByUserIdWithOwner(999L);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 채팅방으로 메시지 조회")
        void findMessagesInNonExistentChatRoom() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<ChatMessage> result = chatMessageRepository.findByChatRoomIdWithSender(999L, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("삭제된 메시지는 개수에서 제외")
        void countExcludesDeletedMessages() {
            // Given
            testMessage.softDelete();
            entityManager.merge(testMessage);
            entityManager.flush();

            // When
            long count = chatMessageRepository.countByChatRoomIdAndIsDeletedFalse(testChatRoom1.getId());

            // Then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("삭제된 파일은 조회에서 제외")
        void findExcludesDeletedFiles() {
            // Given
            testFile.softDelete();
            entityManager.merge(testFile);
            entityManager.flush();

            // When
            Optional<ChatFile> result = chatFileRepository.findByIdAndIsDeletedFalse(testFile.getId());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("비활성화된 채팅방은 초대 코드로 조회되지 않음")
        void inactiveChatRoomNotFoundByInviteCode() {
            // Given
            String inviteCode = "test-invite-code";
            testChatRoom1.generateInviteCode(inviteCode, LocalDateTime.now().plusHours(24));
            testChatRoom1.deactivate();
            entityManager.merge(testChatRoom1);
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<ChatRoom> result = chatRoomRepository.findByInviteCodeAndIsActiveTrue(inviteCode);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 검색어로 채팅방 검색")
        void searchChatRoomsWithEmptyKeyword() {
            // When
            List<ChatRoom> result = chatRoomRepository.searchByName("");

            // Then
            assertThat(result).hasSize(3); // 모든 채팅방이 조회됨
        }

        @Test
        @DisplayName("대소문자 구분없이 채팅방 검색")
        void searchChatRoomsCaseInsensitive() {
            // When
            List<ChatRoom> result = chatRoomRepository.searchByName("테스트");

            // Then
            assertThat(result).hasSize(2);
        }
    }
}