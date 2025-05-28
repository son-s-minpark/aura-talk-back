package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class InvalidChatRoomStateException extends BusinessException {
    private InvalidChatRoomStateException() {
        super(ErrorCode.CHAT_ACCESS_DENIED);
    }

    private InvalidChatRoomStateException(String message) {
        super(ErrorCode.CHAT_ACCESS_DENIED, message);
    }

    public static InvalidChatRoomStateException deactivated() {
        return new InvalidChatRoomStateException("비활성화된 채팅방에서는 작업을 수행할 수 없습니다.");
    }

    public static InvalidChatRoomStateException alreadyMember() {
        return new InvalidChatRoomStateException("이미 채팅방에 참여중입니다.");
    }

    public static InvalidChatRoomStateException notMember() {
        return new InvalidChatRoomStateException("채팅방에 참여하고 있지 않습니다.");
    }

    public static InvalidChatRoomStateException ownerRequired() {
        return new InvalidChatRoomStateException("방장만 수행할 수 있는 작업입니다.");
    }
}