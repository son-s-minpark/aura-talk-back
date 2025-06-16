package com.sonsminpark.auratalkback.domain.friend.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 차단된 친구 관계를 찾을 수 없을 때 발생하는 예외
 */
public class FriendBlockNotFoundException extends BusinessException {

    private FriendBlockNotFoundException(String message) {
        super(ErrorCode.FRIEND_NOT_FOUND, message);
    }

    public static FriendBlockNotFoundException between(Long requesterId, Long recipientId) {
        return new FriendBlockNotFoundException("차단 관계를 찾을 수 없습니다. 요청자 ID: " + requesterId + ", 수신자 ID: " + recipientId);
    }

}
