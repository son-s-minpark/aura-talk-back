package com.sonsminpark.auratalkback.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 코드 규칙
 * - 4xx: 클라이언트 에러
 *   - 400~409: 일반적인 요청 에러
 *   - 410~419: 인증/인가 관련 에러
 *   - 420~429: 사용자 관련 에러
 *   - 430~439: 친구 관련 에러
 *   - 440~449: 채팅 관련 에러
 * - 5xx: 서버 에러
 *   - 500~509: 일반적인 서버 에러
 *   - 510~519: 데이터베이스 관련 에러
 */
@Getter
public enum ErrorCode {

    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 400, "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 401, "허용되지 않은 메서드입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, 402, "잘못된 타입의 값입니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, 403, "필수 요청 파라미터가 누락되었습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, 404, "엔티티를 찾을 수 없습니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 405, "지원하지 않는 미디어 타입입니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 410, "인증이 필요합니다."),
    INVALID_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, 411, "잘못된 인증 토큰입니다."),
    EXPIRED_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, 412, "만료된 인증 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, 413, "접근이 거부되었습니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 420, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, 421, "이미 사용 중인 이메일입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, 422, "이미 사용 중인 사용자명입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, 423, "이미 사용 중인 닉네임입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 424, "이메일 또는 비밀번호가 올바르지 않습니다."),

    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, 430, "친구를 찾을 수 없습니다."),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, 431, "친구 요청을 찾을 수 없습니다."),
    DUPLICATE_FRIEND_REQUEST(HttpStatus.CONFLICT, 432, "이미 친구 요청을 보냈습니다."),
    ALREADY_FRIEND(HttpStatus.CONFLICT, 433, "이미 친구입니다."),

    CHATROOM_NOT_FOUND(HttpStatus.NOT_FOUND, 440, "채팅방을 찾을 수 없습니다."),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, 441, "채팅방에 접근할 권한이 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, 442, "채팅 메시지를 찾을 수 없습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "서버 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, 501, "서비스를 사용할 수 없습니다."),

    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 510, "데이터베이스 오류가 발생했습니다."),

    FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 520, "파일 업로드 중 오류가 발생했습니다."),
    FILE_DOWNLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 521, "파일 다운로드 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;

    ErrorCode(HttpStatus status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}