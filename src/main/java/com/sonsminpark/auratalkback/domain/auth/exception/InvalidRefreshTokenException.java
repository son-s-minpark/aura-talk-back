package com.sonsminpark.auratalkback.domain.auth.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class InvalidRefreshTokenException extends BusinessException {
    private InvalidRefreshTokenException(String message) {
        super(ErrorCode.INVALID_REFRESH_TOKEN, message);
    }

    public static InvalidRefreshTokenException of(String message) {
        return new InvalidRefreshTokenException(message);
    }

    public static InvalidRefreshTokenException of() {
        return new InvalidRefreshTokenException("유효하지 않은 Refresh Token입니다.");
    }
}