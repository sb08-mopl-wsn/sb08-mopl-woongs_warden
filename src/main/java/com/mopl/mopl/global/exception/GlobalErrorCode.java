package com.mopl.mopl.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    INVALID_INPUT(9001, "INVALID_INPUT", HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    FILE_UPLOAD_FAILED(9002, "FILE_UPLOAD_FAILED", HttpStatus.BAD_REQUEST, "파일 업로드에 실패했습니다."),
    SSE_CONNECTION_FAILED(9003, "SSE_CONNECTION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "SSE 연결 초기화 중 에러가 발생했습니다."),
    UNAUTHORIZED(9004, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "인증 정보가 필요하거나 유효하지 않습니다."),
    FORBIDDEN(9005, "FORBIDDEN", HttpStatus.FORBIDDEN, "해당 리소스에 접근할 권한이 없습니다."),
    INTERNAL_SERVER_ERROR(9999, "SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "GLOBAL";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}