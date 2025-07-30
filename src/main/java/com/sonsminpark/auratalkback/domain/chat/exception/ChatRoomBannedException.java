package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class ChatRoomBannedException extends BusinessException {
    private ChatRoomBannedException() {
        super(ErrorCode.CHAT_ACCESS_DENIED);
    }

    private ChatRoomBannedException(String message) {
        super(ErrorCode.CHAT_ACCESS_DENIED, message);
    }

    public static ChatRoomBannedException of() {
        return new ChatRoomBannedException("강퇴된 채팅방에는 다시 입장할 수 없습니다.");
    }

    public static ChatRoomBannedException of(String chatRoomName) {
        return new ChatRoomBannedException("'" + chatRoomName + "' 채팅방에서 강퇴되어 입장할 수 없습니다.");
    }
}