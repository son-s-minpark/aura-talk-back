package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Chat Exception 테스트")
class ChatExceptionTest {

    @Nested
    @DisplayName("ChatAccessDeniedException 테스트")
    class ChatAccessDeniedExceptionTest {

        @Test
        @DisplayName("기본 생성자로 예외 생성")
        void createException_Default() {
            // When
            ChatAccessDeniedException exception = ChatAccessDeniedException.of();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED.getMessage());
        }

        @Test
        @DisplayName("커스텀 메시지로 예외 생성")
        void createException_WithMessage() {
            // Given
            String customMessage = "채팅방에 접근할 권한이 없습니다.";

            // When
            ChatAccessDeniedException exception = ChatAccessDeniedException.of(customMessage);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isEqualTo(customMessage);
        }
    }

    @Nested
    @DisplayName("ChatFileNotFoundException 테스트")
    class ChatFileNotFoundExceptionTest {

        @Test
        @DisplayName("메시지로 예외 생성")
        void createException_WithMessage() {
            // Given
            String message = "파일을 찾을 수 없습니다.";

            // When
            ChatFileNotFoundException exception = ChatFileNotFoundException.of(message);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("파일 ID로 예외 생성")
        void createException_WithFileId() {
            // Given
            Long fileId = 123L;

            // When
            ChatFileNotFoundException exception = ChatFileNotFoundException.of(fileId);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo("파일을 찾을 수 없습니다. ID: " + fileId);
        }
    }

    @Nested
    @DisplayName("ChatFileUploadException 테스트")
    class ChatFileUploadExceptionTest {

        @Test
        @DisplayName("파일 크기 초과 예외 생성")
        void createException_FileSizeExceeded() {
            // Given
            long maxSize = 100 * 1024 * 1024; // 100MB

            // When
            ChatFileUploadException exception = ChatFileUploadException.fileSizeExceeded(maxSize);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_ERROR);
            assertThat(exception.getMessage()).contains("파일 크기가 너무 큽니다");
            assertThat(exception.getMessage()).contains("100MB");
        }
    }

    @Nested
    @DisplayName("ChatInvitationException 테스트")
    class ChatInvitationExceptionTest {

        @Test
        @DisplayName("초대를 찾을 수 없음 예외")
        void createException_NotFound() {
            // When
            ChatInvitationException exception = ChatInvitationException.notFound();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo("초대를 찾을 수 없습니다.");
        }

        @Test
        @DisplayName("이미 처리된 초대 예외")
        void createException_AlreadyProcessed() {
            // When
            ChatInvitationException exception = ChatInvitationException.alreadyProcessed();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
            assertThat(exception.getMessage()).isEqualTo("이미 처리된 초대입니다.");
        }

        @Test
        @DisplayName("중복 초대 예외")
        void createException_DuplicateInvitation() {
            // When
            ChatInvitationException exception = ChatInvitationException.duplicateInvitation();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_FRIEND_REQUEST);
            assertThat(exception.getMessage()).isEqualTo("이미 해당 사용자에게 초대를 보냈습니다.");
        }

        @Test
        @DisplayName("만료된 초대 예외")
        void createException_ExpiredInvite() {
            // When
            ChatInvitationException exception = ChatInvitationException.expiredInvite();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
            assertThat(exception.getMessage()).isEqualTo("만료된 초대 링크입니다.");
        }

        @Test
        @DisplayName("유효하지 않은 초대 코드 예외")
        void createException_InvalidInviteCode() {
            // When
            ChatInvitationException exception = ChatInvitationException.invalidInviteCode();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo("유효하지 않거나 만료된 초대 코드입니다.");
        }
    }

    @Nested
    @DisplayName("ChatRoomBannedException 테스트")
    class ChatRoomBannedExceptionTest {

        @Test
        @DisplayName("기본 차단 예외")
        void createException_Default() {
            // When
            ChatRoomBannedException exception = ChatRoomBannedException.of();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isEqualTo("강퇴된 채팅방에는 다시 입장할 수 없습니다.");
        }

        @Test
        @DisplayName("채팅방 이름을 포함한 차단 예외")
        void createException_WithChatRoomName() {
            // Given
            String chatRoomName = "테스트 채팅방";

            // When
            ChatRoomBannedException exception = ChatRoomBannedException.of(chatRoomName);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isEqualTo("'" + chatRoomName + "' 채팅방에서 강퇴되어 입장할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("ChatRoomNotFoundException 테스트")
    class ChatRoomNotFoundExceptionTest {

        @Test
        @DisplayName("기본 예외 생성")
        void createException_Default() {
            // When
            ChatRoomNotFoundException exception = ChatRoomNotFoundException.of();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo(ErrorCode.CHATROOM_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("채팅방 ID로 예외 생성")
        void createException_WithChatRoomId() {
            // Given
            Long chatRoomId = 456L;

            // When
            ChatRoomNotFoundException exception = ChatRoomNotFoundException.of(chatRoomId);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo("채팅방을 찾을 수 없습니다. ID: " + chatRoomId);
        }
    }

    @Nested
    @DisplayName("InvalidChatRoomStateException 테스트")
    class InvalidChatRoomStateExceptionTest {

        @Test
        @DisplayName("비활성화된 채팅방 예외")
        void createException_Deactivated() {
            // When
            InvalidChatRoomStateException exception = InvalidChatRoomStateException.deactivated();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isEqualTo("비활성화된 채팅방에서는 작업을 수행할 수 없습니다.");
        }

        @Test
        @DisplayName("이미 멤버인 경우 예외")
        void createException_AlreadyMember() {
            // When
            InvalidChatRoomStateException exception = InvalidChatRoomStateException.alreadyMember();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isEqualTo("이미 채팅방에 참여중입니다.");
        }

        @Test
        @DisplayName("멤버가 아닌 경우 예외")
        void createException_NotMember() {
            // When
            InvalidChatRoomStateException exception = InvalidChatRoomStateException.notMember();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isEqualTo("채팅방에 참여하고 있지 않습니다.");
        }

        @Test
        @DisplayName("방장 권한 필요 예외")
        void createException_OwnerRequired() {
            // When
            InvalidChatRoomStateException exception = InvalidChatRoomStateException.ownerRequired();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isEqualTo("방장만 수행할 수 있는 작업입니다.");
        }
    }

    @Nested
    @DisplayName("MessageNotFoundException 테스트")
    class MessageNotFoundExceptionTest {

        @Test
        @DisplayName("기본 예외 생성")
        void createException_Default() {
            // When
            MessageNotFoundException exception = MessageNotFoundException.of();

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("메시지 ID로 예외 생성")
        void createException_WithMessageId() {
            // Given
            Long messageId = 789L;

            // When
            MessageNotFoundException exception = MessageNotFoundException.of(messageId);

            // Then
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
            assertThat(exception.getMessage()).isEqualTo("메시지를 찾을 수 없습니다. ID: " + messageId);
        }
    }

    @Nested
    @DisplayName("예외 체이닝 테스트")
    class ExceptionChainingTest {

        @Test
        @DisplayName("예외가 BusinessException을 상속받는지 확인")
        void checkInheritance() {
            // Given & When
            ChatAccessDeniedException accessException = ChatAccessDeniedException.of();
            ChatRoomNotFoundException notFoundException = ChatRoomNotFoundException.of();
            MessageNotFoundException messageException = MessageNotFoundException.of();

            // Then
            assertThat(accessException).isInstanceOf(RuntimeException.class);
            assertThat(notFoundException).isInstanceOf(RuntimeException.class);
            assertThat(messageException).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("예외의 ErrorCode가 올바르게 설정되는지 확인")
        void checkErrorCodes() {
            // Given & When
            ChatAccessDeniedException accessException = ChatAccessDeniedException.of();
            ChatRoomNotFoundException roomException = ChatRoomNotFoundException.of();
            ChatFileNotFoundException fileException = ChatFileNotFoundException.of("test");
            MessageNotFoundException messageException = MessageNotFoundException.of();

            // Then
            assertThat(accessException.getErrorCode()).isEqualTo(ErrorCode.CHAT_ACCESS_DENIED);
            assertThat(roomException.getErrorCode()).isEqualTo(ErrorCode.CHATROOM_NOT_FOUND);
            assertThat(fileException.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
            assertThat(messageException.getErrorCode()).isEqualTo(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("경계값 테스트")
    class BoundaryValueTest {

        @Test
        @DisplayName("빈 문자열 메시지로 예외 생성")
        void createException_EmptyMessage() {
            // Given
            String emptyMessage = "";

            // When
            ChatFileNotFoundException exception = ChatFileNotFoundException.of(emptyMessage);

            // Then
            assertThat(exception.getMessage()).isEqualTo(emptyMessage);
        }

        @Test
        @DisplayName("null 메시지로 예외 생성")
        void createException_NullMessage() {
            // Given
            String nullMessage = null;

            // When
            ChatFileNotFoundException exception = ChatFileNotFoundException.of(nullMessage);

            // Then
            assertThat(exception.getMessage()).isNull();
        }

        @Test
        @DisplayName("매우 긴 메시지로 예외 생성")
        void createException_VeryLongMessage() {
            // Given
            String longMessage = "A".repeat(1000);

            // When
            ChatFileNotFoundException exception = ChatFileNotFoundException.of(longMessage);

            // Then
            assertThat(exception.getMessage()).isEqualTo(longMessage);
            assertThat(exception.getMessage().length()).isEqualTo(1000);
        }

        @Test
        @DisplayName("특수 문자가 포함된 메시지로 예외 생성")
        void createException_SpecialCharacters() {
            // Given
            String specialMessage = "파일을 찾을 수 없습니다: @#$%^&*()_+{}|:<>?[];',./";

            // When
            ChatFileNotFoundException exception = ChatFileNotFoundException.of(specialMessage);

            // Then
            assertThat(exception.getMessage()).isEqualTo(specialMessage);
        }

        @Test
        @DisplayName("유니코드 문자가 포함된 메시지로 예외 생성")
        void createException_UnicodeCharacters() {
            // Given
            String unicodeMessage = "파일을 찾을 수 없습니다: 🚫📁❌";

            // When
            ChatFileNotFoundException exception = ChatFileNotFoundException.of(unicodeMessage);

            // Then
            assertThat(exception.getMessage()).isEqualTo(unicodeMessage);
        }

        @Test
        @DisplayName("ID 경계값 테스트 - 최소값")
        void createException_MinIdValue() {
            // Given
            Long minId = 1L;

            // When
            ChatRoomNotFoundException exception = ChatRoomNotFoundException.of(minId);

            // Then
            assertThat(exception.getMessage()).contains(minId.toString());
        }

        @Test
        @DisplayName("ID 경계값 테스트 - 최대값")
        void createException_MaxIdValue() {
            // Given
            Long maxId = Long.MAX_VALUE;

            // When
            ChatRoomNotFoundException exception = ChatRoomNotFoundException.of(maxId);

            // Then
            assertThat(exception.getMessage()).contains(maxId.toString());
        }

        @Test
        @DisplayName("파일 크기 경계값 테스트 - 0MB")
        void createException_ZeroFileSize() {
            // Given
            long zeroSize = 0L;

            // When
            ChatFileUploadException exception = ChatFileUploadException.fileSizeExceeded(zeroSize);

            // Then
            assertThat(exception.getMessage()).contains("0MB");
        }

        @Test
        @DisplayName("파일 크기 경계값 테스트 - 매우 큰 크기")
        void createException_VeryLargeFileSize() {
            // Given
            long largeSize = 10L * 1024 * 1024 * 1024; // 10GB

            // When
            ChatFileUploadException exception = ChatFileUploadException.fileSizeExceeded(largeSize);

            // Then
            assertThat(exception.getMessage()).contains("10240MB");
        }
    }
}