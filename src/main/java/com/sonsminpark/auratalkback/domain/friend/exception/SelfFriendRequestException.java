package com.sonsminpark.auratalkback.domain.friend.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 자기 자신에게 친구 요청을 보낼 때 발생하는 예외
 */
public class SelfFriendRequestException extends BusinessException {

    private SelfFriendRequestException(String message) {
        super(ErrorCode.SELF_FRIEND_REQUEST, message);
    }

    public static SelfFriendRequestException create() {
        return new SelfFriendRequestException("자기 자신에게 친구 요청을 보낼 수 없습니다.");
    }
}
