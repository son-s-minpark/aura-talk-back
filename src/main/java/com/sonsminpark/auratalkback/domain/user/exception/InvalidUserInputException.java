package com.sonsminpark.auratalkback.domain.user.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 사용자 관련 입력값 오류가 발생했을 때 발생하는 예외
 */
public class InvalidUserInputException extends BusinessException {
    private InvalidUserInputException() {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }

    private InvalidUserInputException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE, message);
    }

    public static InvalidUserInputException of() {
        return new InvalidUserInputException();
    }

    public static InvalidUserInputException of(String message) {
        return new InvalidUserInputException(message);
    }
}