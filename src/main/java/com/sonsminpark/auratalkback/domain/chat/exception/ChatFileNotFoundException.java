package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class ChatFileNotFoundException extends BusinessException {
    private ChatFileNotFoundException(String message) {
        super(ErrorCode.ENTITY_NOT_FOUND, message);
    }

    public static ChatFileNotFoundException of(String message) {
        return new ChatFileNotFoundException(message);
    }

    public static ChatFileNotFoundException of(Long fileId) {
        return new ChatFileNotFoundException("파일을 찾을 수 없습니다. ID: " + fileId);
    }
}