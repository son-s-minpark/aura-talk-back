package com.sonsminpark.auratalkback.domain.chat.dto.response;

import com.sonsminpark.auratalkback.domain.chat.entity.ChatFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatFileResponseDto {

    private Long fileId;
    private Long chatroomId;
    private Long messageId;
    private String originalFileName;
    private String fileExtension;
    private String mimeType;
    private Long fileSize;
    private String formattedFileSize;
    private boolean isImage;
    private boolean isVideo;
    private boolean isAudio;
    private ChatUserResponseDto uploader;
    private LocalDateTime createdAt;
    private boolean isDeleted;

    public static ChatFileResponseDto from(ChatFile chatFile) {
        return ChatFileResponseDto.builder()
                .fileId(chatFile.getId())
                .chatroomId(chatFile.getChatRoom().getId())
                .messageId(chatFile.getMessage() != null ? chatFile.getMessage().getId() : null)
                .originalFileName(chatFile.getOriginalFileName())
                .fileExtension(chatFile.getFileExtension())
                .mimeType(chatFile.getMimeType())
                .fileSize(chatFile.getFileSize())
                .formattedFileSize(chatFile.getFormattedFileSize())
                .isImage(chatFile.isImage())
                .isVideo(chatFile.isVideo())
                .isAudio(chatFile.isAudio())
                .uploader(ChatUserResponseDto.from(chatFile.getUploader()))
                .createdAt(chatFile.getCreatedAt())
                .isDeleted(chatFile.isDeleted())
                .build();
    }

    public static ChatFileResponseDto from(ChatFile chatFile, String uploaderThumbnailUrl) {
        ChatFileResponseDto dto = from(chatFile);
        if (dto.getUploader() != null) {
            dto.getUploader().setThumbnailImageUrl(uploaderThumbnailUrl);
        }
        return dto;
    }
}