package com.sonsminpark.auratalkback.domain.user.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

public class ImageSizeLimitExceededException extends BusinessException {
    public ImageSizeLimitExceededException(String message) {
        super(ErrorCode.FILE_UPLOAD_ERROR, message);
    }
    public static ImageSizeLimitExceededException of(String message) {
        return new ImageSizeLimitExceededException(message);
    }
}