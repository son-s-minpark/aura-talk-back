package com.sonsminpark.auratalkback.domain.user.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 사용자 관련 입력값 오류가 발생했을 때 발생하는 예외
 */
public class InvalidUserInputException extends BusinessException {
    public InvalidUserInputException() {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }

    public InvalidUserInputException(String message) {
        super(ErrorCode.INVALID_INPUT_VALUE, message);
    }
}