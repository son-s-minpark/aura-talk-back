package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatFileUploadRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileDownloadResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileResponseDto;
import com.sonsminpark.auratalkback.domain.chat.entity.*;
import com.sonsminpark.auratalkback.domain.chat.exception.*;
import com.sonsminpark.auratalkback.domain.chat.repository.*;
import com.sonsminpark.auratalkback.domain.user.entity.User;
import com.sonsminpark.auratalkback.domain.user.entity.UserProfileImage;
import com.sonsminpark.auratalkback.domain.user.entity.UserStatus;
import com.sonsminpark.auratalkback.domain.user.exception.UserNotFoundException;
import com.sonsminpark.auratalkback.domain.user.repository.UserRepository;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "cloud.aws.s3.bucket=test-bucket"
})
@DisplayName("ChatFileService 테스트")
class ChatFileServiceImplTest {

    @Mock
    private ChatFileRepository chatFileRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomUserRepository chatRoomUserRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private ChatFileServiceImpl chatFileService;

    private User testUser;
    private ChatRoom testChatRoom;
    private ChatRoomUser testRoomUser;
    private ChatFile testChatFile;
    private ChatMessage testMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatFileService, "bucketName", "test-bucket");

        // 테스트용 사용자 생성
        testUser = createTestUser();

        // 테스트용 채팅방 생성
        testChatRoom = createTestChatRoom();

        // 테스트용 채팅방 사용자 관계 생성
        testRoomUser = createTestChatRoomUser();

        // 테스트용 메시지 생성
        testMessage = createTestMessage();

        // 테스트용 파일 생성
        testChatFile = createTestChatFile();
    }

    private User createTestUser() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .nickname("테스트유저")
                .password("encodedPassword")
                .status(UserStatus.ONLINE)
                .isDeleted(false)
                .build();

        UserProfileImage profileImage = UserProfileImage.builder()
                .user(user)
                .originalImageUrl("http://test.com/profile.png")
                .thumbnailImageUrl("http://test.com/profile_thumb.png")
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
                .owner(testUser)
                .isActive(true)
                .build();
    }

    private ChatRoomUser createTestChatRoomUser() {
        return ChatRoomUser.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .user(testUser)
                .notificationEnabled(true)
                .build();
    }

    private ChatMessage createTestMessage() {
        return ChatMessage.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .sender(testUser)
                .content("test.jpg")
                .type(MessageType.IMAGE)
                .build();
    }

    private ChatFile createTestChatFile() {
        return ChatFile.builder()
                .id(1L)
                .chatRoom(testChatRoom)
                .uploader(testUser)
                .message(testMessage)
                .originalFileName("test.jpg")
                .s3Key("chat-files/test.jpg")
                .s3Url("https://test-bucket.s3.amazonaws.com/chat-files/test.jpg")
                .fileExtension("jpg")
                .mimeType("image/jpeg")
                .fileSize(1024L)
                .isDeleted(false)
                .build();
    }

    @Nested
    @DisplayName("채팅방 접근 권한 검증 테스트")
    class ValidateChatRoomAccessTest {

        @Test
        @DisplayName("성공: 유효한 채팅방 접근")
        void validateChatRoomAccess_Success() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).thenReturn(List.of(testRoomUser));

            // When + Then (예외가 발생하지 않아야 함)
            chatFileService.validateChatRoomAccess(1L, 1L);

            verify(chatRoomRepository).findById(1L);
            verify(chatRoomUserRepository).findByChatRoomIdAndUserId(1L, 1L);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 채팅방")
        void validateChatRoomAccess_ChatRoomNotFound() {
            // Given
            when(chatRoomRepository.findById(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatFileService.validateChatRoomAccess(999L, 1L))
                    .isInstanceOf(ChatRoomNotFoundException.class);
        }

        @Test
        @DisplayName("실패: 비활성화된 채팅방")
        void validateChatRoomAccess_InactiveChatRoom() {
            // Given
            ChatRoom inactiveChatRoom = ChatRoom.builder()
                    .id(1L)
                    .name("비활성 채팅방")
                    .type(ChatRoomType.GROUP)
                    .owner(testUser)
                    .isActive(false)
                    .build();

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(inactiveChatRoom));

            // When + Then
            assertThatThrownBy(() -> chatFileService.validateChatRoomAccess(1L, 1L))
                    .isInstanceOf(InvalidChatRoomStateException.class);
        }

        @Test
        @DisplayName("실패: 채팅방 멤버가 아닌 사용자")
        void validateChatRoomAccess_NotMember() {
            // Given
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 2L)).thenReturn(List.of());

            // When + Then
            assertThatThrownBy(() -> chatFileService.validateChatRoomAccess(1L, 2L))
                    .isInstanceOf(ChatAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("파일 업로드 완료 처리 테스트")
    class CompleteFileUploadTest {

        @Test
        @DisplayName("성공: 이미지 파일 업로드")
        void completeFileUpload_ImageFile_Success() {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/test.jpg")
                    .originalFileName("test.jpg")
                    .fileSize(1024L)
                    .build();

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).thenReturn(List.of(testRoomUser));
            when(chatFileRepository.save(any(ChatFile.class))).thenReturn(testChatFile);
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(testMessage);

            // When
            ChatFileResponseDto result = chatFileService.completeFileUpload(1L, 1L, requestDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalFileName()).isEqualTo("test.jpg");
            assertThat(result.getFileExtension()).isEqualTo("jpg");
            assertThat(result.getMimeType()).isEqualTo("image/jpeg");
            assertThat(result.isImage()).isTrue();

            verify(chatFileRepository).save(any(ChatFile.class));
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/chatroom/1/files"), any(ChatFileResponseDto.class));
        }

        @Test
        @DisplayName("실패: 파일 크기 초과")
        void completeFileUpload_FileSizeExceeded() {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/large.jpg")
                    .originalFileName("large.jpg")
                    .fileSize(200L * 1024 * 1024) // 200MB
                    .build();

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testUser));
            when(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).thenReturn(List.of(testRoomUser));

            // When + Then
            assertThatThrownBy(() -> chatFileService.completeFileUpload(1L, 1L, requestDto))
                    .isInstanceOf(ChatFileUploadException.class);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void completeFileUpload_UserNotFound() {
            // Given
            ChatFileUploadRequestDto requestDto = ChatFileUploadRequestDto.builder()
                    .s3Key("chat-files/test.jpg")
                    .originalFileName("test.jpg")
                    .fileSize(1024L)
                    .build();

            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(userRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatFileService.completeFileUpload(1L, 999L, requestDto))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("파일 다운로드 URL 생성 테스트")
    class GenerateDownloadUrlTest {

        @Test
        @DisplayName("성공: 다운로드 URL 생성")
        void generateDownloadUrl_Success() throws Exception {
            // Given
            URL mockUrl = new URL("https://test-bucket.s3.amazonaws.com/presigned-url");
            PresignedGetObjectRequest mockPresignedRequest = mock(PresignedGetObjectRequest.class);

            when(chatFileRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testChatFile));
            when(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).thenReturn(List.of(testRoomUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignedRequest);
            when(mockPresignedRequest.url()).thenReturn(mockUrl);

            // When
            ChatFileDownloadResponseDto result = chatFileService.generateDownloadUrl(1L, 1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getFileId()).isEqualTo(1L);
            assertThat(result.getOriginalFileName()).isEqualTo("test.jpg");
            assertThat(result.getDownloadUrl()).isEqualTo(mockUrl.toString());
            assertThat(result.getFileSize()).isEqualTo(1024L);
            assertThat(result.getMimeType()).isEqualTo("image/jpeg");
            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());

            verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 파일")
        void generateDownloadUrl_FileNotFound() {
            // Given
            when(chatFileRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatFileService.generateDownloadUrl(999L, 1L))
                    .isInstanceOf(ChatFileNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("채팅방 파일 목록 조회 테스트")
    class GetChatRoomFilesTest {

        @Test
        @DisplayName("성공: 파일 목록 조회")
        void getChatRoomFiles_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            List<ChatFile> files = List.of(testChatFile);
            Page<ChatFile> filePage = new PageImpl<>(files, pageable, 1);

            when(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).thenReturn(List.of(testRoomUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatFileRepository.findByChatRoomIdWithUploader(eq(1L), any(Pageable.class))).thenReturn(filePage);

            // When
            List<ChatFileResponseDto> result = chatFileService.getChatRoomFiles(1L, 1L, 0, 20);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginalFileName()).isEqualTo("test.jpg");
            assertThat(result.get(0).getUploader().getNickname()).isEqualTo("테스트유저");

            verify(chatFileRepository).findByChatRoomIdWithUploader(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("성공: 빈 파일 목록")
        void getChatRoomFiles_EmptyList() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            Page<ChatFile> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(chatRoomUserRepository.findByChatRoomIdAndUserId(1L, 1L)).thenReturn(List.of(testRoomUser));
            when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(testChatRoom));
            when(chatFileRepository.findByChatRoomIdWithUploader(eq(1L), any(Pageable.class))).thenReturn(emptyPage);

            // When
            List<ChatFileResponseDto> result = chatFileService.getChatRoomFiles(1L, 1L, 0, 20);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("파일 삭제 테스트")
    class DeleteFileTest {

        @Test
        @DisplayName("성공: 파일 삭제")
        void deleteFile_Success() {
            // Given
            when(chatFileRepository.findByIdAndUploaderIdAndIsDeletedFalse(1L, 1L)).thenReturn(Optional.of(testChatFile));

            // When
            chatFileService.deleteFile(1L, 1L);

            // Then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/chatroom/1/files/deleted"),
                    any(ChatFileResponseDto.class)
            );
        }

        @Test
        @DisplayName("실패: 존재하지 않는 파일")
        void deleteFile_FileNotFound() {
            // Given
            when(chatFileRepository.findByIdAndUploaderIdAndIsDeletedFalse(999L, 1L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatFileService.deleteFile(999L, 1L))
                    .isInstanceOf(ChatFileNotFoundException.class);
        }

        @Test
        @DisplayName("실패: 다른 사용자의 파일 삭제 시도")
        void deleteFile_NotOwner() {
            // Given
            when(chatFileRepository.findByIdAndUploaderIdAndIsDeletedFalse(1L, 2L)).thenReturn(Optional.empty());

            // When + Then
            assertThatThrownBy(() -> chatFileService.deleteFile(1L, 2L))
                    .isInstanceOf(ChatFileNotFoundException.class);
        }
    }
}