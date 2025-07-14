package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomImageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomImageServiceImpl implements ChatRoomImageService {

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

        int index = Math.toIntExact(chatRoomId % DEFAULT_GROUP_IMAGE_COUNT) + 1;
        String prefix = "https://" + bucketName + ".s3.amazonaws.com/group-images/default/";

        String originalImageUrl = prefix + index + ".png";
        String thumbnailImageUrl = prefix + index + "_thumb.png";

        log.debug("채팅방 기본 이미지 생성 완료 - 원본: {}", originalImageUrl);

        return ChatRoomImageResponseDto.builder()
                .originalImageUrl(originalImageUrl)
                .thumbnailImageUrl(thumbnailImageUrl)
                .isDefaultImage(true)
                .build();
    }
}