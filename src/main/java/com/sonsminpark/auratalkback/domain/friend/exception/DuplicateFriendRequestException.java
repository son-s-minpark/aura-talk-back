package com.sonsminpark.auratalkback.domain.friend.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 중복된 친구 요청을 보낼 때 발생하는 예외
 */
public class DuplicateFriendRequestException extends BusinessException {

    private DuplicateFriendRequestException(String message) {
        super(ErrorCode.DUPLICATE_FRIEND_REQUEST, message);
    }

    public static DuplicateFriendRequestException between(Long requesterId, Long recipientId) {
        return new DuplicateFriendRequestException("이미 보낸 친구 요청입니다. 요청자 ID: " + requesterId + ", 수신자 ID: " + recipientId);
    }
}
