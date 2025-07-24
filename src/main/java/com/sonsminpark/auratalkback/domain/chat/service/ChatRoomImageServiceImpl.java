package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomImageResponseDto;
import com.sonsminpark.auratalkback.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomImageServiceImpl implements ChatRoomImageService {

    private final S3Service s3Service;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private static final int DEFAULT_GROUP_IMAGE_COUNT = 2;

    @Override
    public ChatRoomImageResponseDto processUploadedImage(String s3Key) {
        log.info("채팅방 이미지 업로드 완료 처리 - S3 키: {}", s3Key);

        String originalImageUrl = "https://" + bucketName + ".s3.amazonaws.com/" + s3Key;
        String thumbnailImageUrl = originalImageUrl.replace("/original/", "/thumbnail/");

        log.info("채팅방 이미지 URL 생성 완료 - 원본: {}, 썸네일: {}", originalImageUrl, thumbnailImageUrl);

        return ChatRoomImageResponseDto.builder()
                .originalImageUrl(originalImageUrl)
                .thumbnailImageUrl(thumbnailImageUrl)
                .isDefaultImage(false)
                .build();
    }

    @Override
    public ChatRoomImageResponseDto getDefaultImage(Long chatRoomId) {
        log.debug("채팅방 기본 이미지 생성 - 채팅방 ID: {}", chatRoomId);

        try {
            int index = (int) (chatRoomId % DEFAULT_GROUP_IMAGE_COUNT) + 1;

            // 범위 체크
            if (index < 1 || index > DEFAULT_GROUP_IMAGE_COUNT) {
                index = 1;
            }

            String prefix = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/";

            String originalImageUrl = prefix + index + ".png";
            String thumbnailImageUrl = prefix + index + "_thumb.png";

            log.debug("채팅방 기본 이미지 생성 완료 - 원본: {}, 썸네일: {}", originalImageUrl, thumbnailImageUrl);

            return ChatRoomImageResponseDto.builder()
                    .originalImageUrl(originalImageUrl)
                    .thumbnailImageUrl(thumbnailImageUrl)
                    .isDefaultImage(true)
                    .build();
        } catch (Exception e) {
            log.error("채팅방 기본 이미지 생성 실패 - ID: {}, 에러: {}", chatRoomId, e.getMessage(), e);

            // 오류 발생 시 기본값
            String prefix = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/";
            return ChatRoomImageResponseDto.builder()
                    .originalImageUrl(prefix + "1.png")
                    .thumbnailImageUrl(prefix + "1_thumb.png")
                    .isDefaultImage(true)
                    .build();
        }
    }

    public ChatRoomImageResponseDto deleteRoomImage(Long chatRoomId, String currentImageUrl, String currentThumbnailUrl) {
        log.info("채팅방 이미지 삭제 및 기본 이미지로 변경 - 채팅방 ID: {}", chatRoomId);

        try {
            // 현재 이미지가 기본 이미지가 아닌 경우에만 S3에서 삭제
            if (currentImageUrl != null && !isDefaultImage(currentImageUrl)) {
                // 원본 이미지 삭제
                s3Service.deleteFileFromS3(currentImageUrl);

                // 썸네일 이미지 삭제 (URL이 있는 경우)
                if (currentThumbnailUrl != null) {
                    s3Service.deleteFileFromS3(currentThumbnailUrl);
                } else {
                    // 썸네일 URL이 없다면 원본 URL에서 생성
                    String thumbnailUrl = currentImageUrl.replace("/original/", "/thumbnail/");
                    s3Service.deleteFileFromS3(thumbnailUrl);
                }

                log.info("S3에서 채팅방 이미지 삭제 완료 - 원본: {}, 썸네일: {}",
                        currentImageUrl, currentThumbnailUrl != null ? currentThumbnailUrl : "자동생성");
            }

            // 기본 이미지로 변경
            ChatRoomImageResponseDto defaultImage = getDefaultImage(chatRoomId);

            log.info("채팅방 이미지를 기본 이미지로 변경 완료 - 채팅방 ID: {}", chatRoomId);

            return defaultImage;
        } catch (Exception e) {
            log.error("채팅방 이미지 삭제 실패 - 채팅방 ID: {}, 에러: {}", chatRoomId, e.getMessage(), e);

            // 오류 발생 시에도 기본 이미지 반환
            return getDefaultImage(chatRoomId);
        }
    }

    private boolean isDefaultImage(String imageUrl) {
        if (imageUrl == null) {
            return true;
        }

        String defaultImagePrefix = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/";
        return imageUrl.startsWith(defaultImagePrefix);
    }
}