package com.sonsminpark.auratalkback.domain.user.exception;

import com.sonsminpark.auratalkback.global.exception.BusinessException;
import com.sonsminpark.auratalkback.global.exception.ErrorCode;

/**
 * 사용자 정보(이메일, 사용자명, 닉네임 등)가 중복될 때 발생하는 예외
 */
public class DuplicateUserException extends BusinessException {

    public DuplicateUserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DuplicateUserException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static DuplicateUserException ofEmail(String email) {
        return new DuplicateUserException(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다: " + email);
    }

    public static DuplicateUserException ofUsername(String username) {
        return new DuplicateUserException(ErrorCode.DUPLICATE_USERNAME, "이미 사용 중인 사용자명입니다: " + username);
    }

    public static DuplicateUserException ofNickname(String nickname) {
        return new DuplicateUserException(ErrorCode.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다: " + nickname);
    }
}