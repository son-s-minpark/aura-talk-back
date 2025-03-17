package com.sonsminpark.auratalkback.domain.user.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 사용자 인증 정보가 잘못되었을 때 발생하는 예외
 */
public class InvalidUserCredentialsException extends BusinessException {
    public InvalidUserCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }

    public InvalidUserCredentialsException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }
}