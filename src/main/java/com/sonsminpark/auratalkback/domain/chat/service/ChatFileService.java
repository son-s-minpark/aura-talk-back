package com.sonsminpark.auratalkback.domain.chat.service;

import com.sonsminpark.auratalkback.domain.chat.dto.request.ChatFileUploadRequestDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileDownloadResponseDto;
import com.sonsminpark.auratalkback.domain.chat.dto.response.ChatFileResponseDto;

import java.util.List;

public interface ChatFileService {

    void validateChatRoomAccess(Long chatroomId, Long userId);

    ChatFileResponseDto completeFileUpload(Long chatroomId, Long userId, ChatFileUploadRequestDto requestDto);

    ChatFileDownloadResponseDto generateDownloadUrl(Long fileId, Long userId);

    List<ChatFileResponseDto> getChatRoomFiles(Long chatroomId, Long userId, int page, int size);

    void deleteFile(Long fileId, Long userId);
}