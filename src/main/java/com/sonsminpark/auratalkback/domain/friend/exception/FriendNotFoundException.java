package com.sonsminpark.auratalkback.domain.friend.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 친구 관계를 찾을 수 없을 때 발생하는 예외
 */
public class FriendNotFoundException extends BusinessException {

    private FriendNotFoundException(String message) {
        super(ErrorCode.FRIEND_NOT_FOUND, message);
    }

    public static FriendNotFoundException between(Long requesterId, Long recipientId) {
        return new FriendNotFoundException("두 사용자 사이의 친구 관계를 찾을 수 없습니다. ID: " + requesterId + "," + recipientId);
    }

}
