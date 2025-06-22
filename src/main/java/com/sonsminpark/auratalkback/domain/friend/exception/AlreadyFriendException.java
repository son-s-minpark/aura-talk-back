package com.sonsminpark.auratalkback.domain.friend.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 이미 친구인 상태에서 친구 요청을 보낼 때 발생하는 예외
 */
public class AlreadyFriendException extends BusinessException {

    private AlreadyFriendException(String message) {
        super(ErrorCode.ALREADY_FRIEND, message);
    }

    public static AlreadyFriendException between(Long requesterId, Long recipientId) {
        return new AlreadyFriendException("두 사용자는 이미 친구입니다. ID: " + requesterId + "," + recipientId);
    }
}
