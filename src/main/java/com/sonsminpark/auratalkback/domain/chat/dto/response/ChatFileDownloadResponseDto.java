package com.sonsminpark.auratalkback.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatFileDownloadResponseDto {

    private Long fileId;
    private String originalFileName;
    private String downloadUrl;
    private LocalDateTime expiresAt;
    private Long fileSize;
    private String formattedFileSize;
    private String mimeType;
}