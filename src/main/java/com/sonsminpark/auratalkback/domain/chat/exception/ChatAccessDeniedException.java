package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class ChatAccessDeniedException extends BusinessException {
    private ChatAccessDeniedException() {
        super(ErrorCode.CHAT_ACCESS_DENIED);
    }

    private ChatAccessDeniedException(String message) {
        super(ErrorCode.CHAT_ACCESS_DENIED, message);
    }

    public static ChatAccessDeniedException of() {
        return new ChatAccessDeniedException();
    }

    public static ChatAccessDeniedException of(String message) {
        return new ChatAccessDeniedException(message);
    }
}