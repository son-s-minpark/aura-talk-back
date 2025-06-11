package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class MessageNotFoundException extends BusinessException {
    private MessageNotFoundException() {
        super(ErrorCode.CHAT_MESSAGE_NOT_FOUND);
    }

    private MessageNotFoundException(String message) {
        super(ErrorCode.CHAT_MESSAGE_NOT_FOUND, message);
    }

    public static MessageNotFoundException of() {
        return new MessageNotFoundException();
    }

    public static MessageNotFoundException of(Long messageId) {
        return new MessageNotFoundException("메시지를 찾을 수 없습니다. ID: " + messageId);
    }
}