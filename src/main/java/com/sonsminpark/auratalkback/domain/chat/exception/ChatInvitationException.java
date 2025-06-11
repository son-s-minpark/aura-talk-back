package com.sonsminpark.auratalkback.domain.chat.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class ChatInvitationException extends BusinessException {
    private ChatInvitationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static ChatInvitationException notFound() {
        return new ChatInvitationException(ErrorCode.ENTITY_NOT_FOUND, "초대를 찾을 수 없습니다.");
    }

    public static ChatInvitationException alreadyProcessed() {
        return new ChatInvitationException(ErrorCode.INVALID_INPUT_VALUE, "이미 처리된 초대입니다.");
    }

    public static ChatInvitationException duplicateInvitation() {
        return new ChatInvitationException(ErrorCode.DUPLICATE_FRIEND_REQUEST, "이미 해당 사용자에게 초대를 보냈습니다.");
    }

    public static ChatInvitationException expiredInvite() {
        return new ChatInvitationException(ErrorCode.INVALID_INPUT_VALUE, "만료된 초대 링크입니다.");
    }

    public static ChatInvitationException invalidInviteCode() {
        return new ChatInvitationException(ErrorCode.ENTITY_NOT_FOUND, "유효하지 않거나 만료된 초대 코드입니다.");
    }
}