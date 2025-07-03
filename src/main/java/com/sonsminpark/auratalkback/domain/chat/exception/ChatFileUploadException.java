package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class ChatFileUploadException extends BusinessException {
    private ChatFileUploadException(String message) {
        super(ErrorCode.FILE_UPLOAD_ERROR, message);
    }

    public static ChatFileUploadException fileSizeExceeded(long maxSize) {
        return new ChatFileUploadException("파일 크기가 너무 큽니다. 최대 " + (maxSize / (1024 * 1024)) + "MB까지 업로드 가능합니다.");
    }
}