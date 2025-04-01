package com.sonsminpark.auratalkback.domain.user.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 */
public class UserNotFoundException extends BusinessException {
    private UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }

    private UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }

    public static UserNotFoundException of() {
        return new UserNotFoundException();
    }

    public static UserNotFoundException of(String message) {
        return new UserNotFoundException(message);
    }

    public static UserNotFoundException of(Long userId) {
        return new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
    }

    public static UserNotFoundException of(String email, String reason) {
        return new UserNotFoundException("사용자를 찾을 수 없습니다. 이메일: " + email + ", 이유: " + reason);
    }
}