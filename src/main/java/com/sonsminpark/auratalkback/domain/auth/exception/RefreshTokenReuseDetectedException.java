package com.sonsminpark.auratalkback.domain.auth.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class RefreshTokenReuseDetectedException extends BusinessException {
    private RefreshTokenReuseDetectedException() {
        super(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
    }

    public static RefreshTokenReuseDetectedException of() {
        return new RefreshTokenReuseDetectedException();
    }
}