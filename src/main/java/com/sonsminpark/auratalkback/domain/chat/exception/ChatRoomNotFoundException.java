package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class ChatRoomNotFoundException extends BusinessException {
    private ChatRoomNotFoundException() {
        super(ErrorCode.CHATROOM_NOT_FOUND);
    }

    private ChatRoomNotFoundException(String message) {
        super(ErrorCode.CHATROOM_NOT_FOUND, message);
    }

    public static ChatRoomNotFoundException of() {
        return new ChatRoomNotFoundException();
    }

    public static ChatRoomNotFoundException of(Long chatRoomId) {
        return new ChatRoomNotFoundException("채팅방을 찾을 수 없습니다. ID: " + chatRoomId);
    }
}