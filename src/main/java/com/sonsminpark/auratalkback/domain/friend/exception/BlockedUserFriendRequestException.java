package com.sonsminpark.auratalkback.domain.friend.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 자신이 차단한 사용자에게 친구 요청을 보낼 때 발생하는 예외
 */
public class BlockedUserFriendRequestException extends BusinessException {

    private BlockedUserFriendRequestException(String message) {
        super(ErrorCode.BLOCKED_USER_FRIEND_REQUEST, message);
    }

    public static BlockedUserFriendRequestException create() {
        return new BlockedUserFriendRequestException("차단한 사용자에게 친구 요청을 보낼 수 없습니다.");
    }
}
