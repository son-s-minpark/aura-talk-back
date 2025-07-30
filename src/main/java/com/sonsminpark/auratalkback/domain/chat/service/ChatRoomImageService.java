package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatRoomImageResponseDto;

public interface ChatRoomImageService {

    // S3 업로드 완료 후 채팅방 이미지 URL 생성
    ChatRoomImageResponseDto processUploadedImage(String s3Key);

    // 채팅방 ID를 기반으로 기본 이미지 정보 생성
    ChatRoomImageResponseDto getDefaultImage(Long chatRoomId);

    // 채팅방 이미지 삭제 및 기본 이미지로 변경
    ChatRoomImageResponseDto deleteRoomImage(Long chatRoomId, String currentImageUrl, String currentThumbnailUrl);
}