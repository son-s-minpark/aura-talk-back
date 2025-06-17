package com.sonsminpark.auratalkback.domain.interest.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class InterestNotFoundException extends BusinessException {

    private InterestNotFoundException() {
        super(ErrorCode.ENTITY_NOT_FOUND);
    }

    public InterestNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static InterestNotFoundException of() {
        return new InterestNotFoundException();
    }

    public static InterestNotFoundException of(String message) {
        return new InterestNotFoundException(ErrorCode.ENTITY_NOT_FOUND, message);
    }

    public static InterestNotFoundException of(String interestName, String reason) {
        return new InterestNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                "관심사를 찾을 수 없습니다. 관심사명: " + interestName + ", 이유: " + reason);
    }
}