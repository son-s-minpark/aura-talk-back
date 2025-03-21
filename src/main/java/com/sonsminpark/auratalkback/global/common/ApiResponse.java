package com.sonsminpark.auratalkback.global.common;

import com.sonsminpark.auratalkback.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private int code;
    private String message;
    private T data;

    // 성공 응답 생성
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(200) // 성공 코드는 200으로 통일
                .message(message)
                .data(data)
                .build();
    }

    // 간단한 성공 응답 생성 (데이터 없음)
    public static ApiResponse<Void> success(String message) {
        return success(message, null);
    }

    // 예외 코드만 사용한 실패 응답 생성
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .data(null)
                .build();
    }

    // 예외 코드와 데이터를 사용한 실패 응답 생성
    public static <T> ApiResponse<T> error(ErrorCode errorCode, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .data(data)
                .build();
    }

    // 예외 코드와 사용자 정의 메시지를 사용한 실패 응답 생성
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getMessage())
                .data(null)
                .build();
    }

    // 예외 코드, 사용자 정의 메시지, 데이터를 사용한 실패 응답 생성
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getMessage())
                .data(data)
                .build();
    }
}