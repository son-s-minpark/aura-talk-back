package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomImageResponseDto;
import com.sonsminpark.auratalkback.global.s3.S3Service;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "cloud.aws.s3.bucket=test-bucket"
})
@DisplayName("ChatRoomImageService 테스트")
class ChatRoomImageServiceImplTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ChatRoomImageServiceImpl chatRoomImageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatRoomImageService, "bucketName", "test-bucket");
    }

    @Nested
    @DisplayName("업로드된 이미지 처리 테스트")
    class ProcessUploadedImageTest {

        @Test
        @DisplayName("성공: S3 업로드 완료된 이미지 처리")
        void processUploadedImage_Success() {
            // Given
            String s3Key = "group-images/original/12345.png";

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.processUploadedImage(s3Key);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/original/12345.png");
            assertThat(result.getThumbnailImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/thumbnail/12345.png");
            assertThat(result.isDefaultImage()).isFalse();
        }

        @Test
        @DisplayName("성공: 복잡한 S3 키 처리")
        void processUploadedImage_ComplexKey() {
            // Given
            String s3Key = "group-images/original/subfolder/image_name_with_underscores.jpg";

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.processUploadedImage(s3Key);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/original/subfolder/image_name_with_underscores.jpg");
            assertThat(result.getThumbnailImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/thumbnail/subfolder/image_name_with_underscores.jpg");
            assertThat(result.isDefaultImage()).isFalse();
        }

        @Test
        @DisplayName("성공: 빈 키 처리")
        void processUploadedImage_EmptyKey() {
            // Given
            String s3Key = "";

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.processUploadedImage(s3Key);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/");
            assertThat(result.getThumbnailImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/");
            assertThat(result.isDefaultImage()).isFalse();
        }

        @Test
        @DisplayName("성공: null 키 처리")
        void processUploadedImage_NullKey() {
            // Given
            String s3Key = null;

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.processUploadedImage(s3Key);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).contains("test-bucket.s3.amazonaws.com");
            assertThat(result.getThumbnailImageUrl()).contains("test-bucket.s3.amazonaws.com");
            assertThat(result.isDefaultImage()).isFalse();
        }
    }

    @Nested
    @DisplayName("기본 이미지 생성 테스트")
    class GetDefaultImageTest {

        @Test
        @DisplayName("성공: 채팅방 ID 1번의 기본 이미지")
        void getDefaultImage_Id1() {
            // Given
            Long chatRoomId = 1L;

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.getDefaultImage(chatRoomId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/2.png");
            assertThat(result.getThumbnailImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/2_thumb.png");
            assertThat(result.isDefaultImage()).isTrue();
        }

        @Test
        @DisplayName("성공: 채팅방 ID 2번의 기본 이미지")
        void getDefaultImage_Id2() {
            // Given
            Long chatRoomId = 2L;

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.getDefaultImage(chatRoomId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/1.png");
            assertThat(result.getThumbnailImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/1_thumb.png");
            assertThat(result.isDefaultImage()).isTrue();
        }

        @Test
        @DisplayName("성공: 채팅방 ID 0번의 기본 이미지 (경계값)")
        void getDefaultImage_IdZero() {
            // Given
            Long chatRoomId = 0L;

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.getDefaultImage(chatRoomId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/1.png");
            assertThat(result.getThumbnailImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/1_thumb.png");
            assertThat(result.isDefaultImage()).isTrue();
        }

        @Test
        @DisplayName("실패 시 fallback: 예외 발생 시 기본값 반환")
        void getDefaultImage_ExceptionFallback() {
            // Given
            Long chatRoomId = null; // null로 인한 예외 발생

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.getDefaultImage(chatRoomId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/1.png");
            assertThat(result.getThumbnailImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/1_thumb.png");
            assertThat(result.isDefaultImage()).isTrue();
        }
    }

    @Nested
    @DisplayName("채팅방 이미지 삭제 테스트")
    class DeleteRoomImageTest {

        @Test
        @DisplayName("성공: 커스텀 이미지 삭제 후 기본 이미지로 변경")
        void deleteRoomImage_CustomImage_Success() {
            // Given
            Long chatRoomId = 5L;
            String currentImageUrl = "https://test-bucket.s3.amazonaws.com/group-images/original/custom.png";
            String currentThumbnailUrl = "https://test-bucket.s3.amazonaws.com/group-images/thumbnail/custom.png";

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.deleteRoomImage(chatRoomId, currentImageUrl, currentThumbnailUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOriginalImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/2.png");
            assertThat(result.getThumbnailImageUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/group-images/default/2_thumb.png");
            assertThat(result.isDefaultImage()).isTrue();

            verify(s3Service).deleteFileFromS3(currentImageUrl);
            verify(s3Service).deleteFileFromS3(currentThumbnailUrl);
        }

        @Test
        @DisplayName("성공: 기본 이미지인 경우 S3 삭제 생략")
        void deleteRoomImage_DefaultImage_SkipS3Delete() {
            // Given
            Long chatRoomId = 5L;
            String currentImageUrl = "https://test-bucket.s3.amazonaws.com/group-images/default/1.png";
            String currentThumbnailUrl = "https://test-bucket.s3.amazonaws.com/group-images/default/1_thumb.png";

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.deleteRoomImage(chatRoomId, currentImageUrl, currentThumbnailUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isDefaultImage()).isTrue();

            verify(s3Service, never()).deleteFileFromS3(anyString());
        }

        @Test
        @DisplayName("성공: null 이미지 URL인 경우")
        void deleteRoomImage_NullImageUrl_Success() {
            // Given
            Long chatRoomId = 5L;
            String currentImageUrl = null;
            String currentThumbnailUrl = null;

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.deleteRoomImage(chatRoomId, currentImageUrl, currentThumbnailUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isDefaultImage()).isTrue();

            verify(s3Service, never()).deleteFileFromS3(anyString());
        }

        @Test
        @DisplayName("성공: S3 삭제 실패 시에도 기본 이미지 반환")
        void deleteRoomImage_S3DeleteFailure_ReturnDefault() {
            // Given
            Long chatRoomId = 5L;
            String currentImageUrl = "https://test-bucket.s3.amazonaws.com/group-images/original/custom.png";
            String currentThumbnailUrl = "https://test-bucket.s3.amazonaws.com/group-images/thumbnail/custom.png";

            doThrow(new RuntimeException("S3 delete failed")).when(s3Service).deleteFileFromS3(anyString());

            // When
            ChatRoomImageResponseDto result = chatRoomImageService.deleteRoomImage(chatRoomId, currentImageUrl, currentThumbnailUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.isDefaultImage()).isTrue();

            verify(s3Service).deleteFileFromS3(currentImageUrl);
        }
    }
}