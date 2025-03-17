package com.sonsminpark.auratalkback.global.exception;

import com.sonsminpark.auratalkback.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 비즈니스 로직 예외 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("BusinessException: code={}, message={}", errorCode.getCode(), e.getMessage(), e);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode, e.getMessage()));
    }

    // 입력값 유효성 검사 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        Map<String, String> errors = new HashMap<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.error("Validation error: {}", errors);
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, "입력값 검증에 실패했습니다.", errors));
    }

    // 메서드 인자 타입 불일치 예외 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        log.error("Type mismatch: {}", ex.getMessage(), ex);
        String message = String.format("잘못된 타입의 값이 입력되었습니다. '%s' 필드에는 '%s' 타입이 필요합니다.",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "알 수 없음");

        return ResponseEntity
                .status(ErrorCode.INVALID_TYPE_VALUE.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_TYPE_VALUE, message));
    }

    // 기타 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception e) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}