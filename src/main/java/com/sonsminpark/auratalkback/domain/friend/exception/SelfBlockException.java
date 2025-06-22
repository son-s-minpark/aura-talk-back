package com.sonsminpark.auratalkback.domain.friend.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 자기 자신을 차단할 때 발생하는 예외
 */
public class SelfBlockException extends BusinessException {

    private SelfBlockException(String message) {
        super(ErrorCode.SELF_FRIEND_REQUEST, message);
    }

    public static SelfBlockException create() {
        return new SelfBlockException("자기 자신을 차단할 수 없습니다.");
    }
}