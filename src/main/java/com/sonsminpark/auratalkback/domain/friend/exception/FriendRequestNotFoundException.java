package com.sonsminpark.auratalkback.domain.friend.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 보낸 친구 요청이 없을 때 발생하는 예외
 */
public class FriendRequestNotFoundException extends BusinessException {

    private FriendRequestNotFoundException(String message) {
        super(ErrorCode.FRIEND_REQUEST_NOT_FOUND, message);
    }

    public static FriendRequestNotFoundException between(Long requesterId, Long recipientId) {
        return new FriendRequestNotFoundException("친구 요청을 찾을 수 없습니다. 요청자 ID: " + requesterId + ", 수신자 ID: " + recipientId);
    }
}
