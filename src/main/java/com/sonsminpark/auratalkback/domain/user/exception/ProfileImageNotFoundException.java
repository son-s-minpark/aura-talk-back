package com.sonsminpark.auratalkback.domain.user.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class ProfileImageNotFoundException extends BusinessException {

    public ProfileImageNotFoundException(String message) {
        super(ErrorCode.ENTITY_NOT_FOUND, message);
    }

    public static ProfileImageNotFoundException of(String message) {
        return new ProfileImageNotFoundException(message);
    }

}