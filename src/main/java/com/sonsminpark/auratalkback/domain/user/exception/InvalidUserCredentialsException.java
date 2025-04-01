package com.sonsminpark.auratalkback.domain.user.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 사용자 인증 정보가 잘못되었을 때 발생하는 예외
 */
public class InvalidUserCredentialsException extends BusinessException {
    private InvalidUserCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }

    private InvalidUserCredentialsException(String message) {
        super(ErrorCode.INVALID_CREDENTIALS, message);
    }

    public static InvalidUserCredentialsException of() {
        return new InvalidUserCredentialsException();
    }

    public static InvalidUserCredentialsException of(String message) {
        return new InvalidUserCredentialsException(message);
    }
}