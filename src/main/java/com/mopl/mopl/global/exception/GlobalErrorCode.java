package com.mopl.mopl.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    INVALID_INPUT(9001, "INVALID_INPUT", HttpStatus.BAD_REQUEST, "입력값 검증에 실패했습니다."),
    FILE_UPLOAD_FAILED(9002, "FILE_UPLOAD_FAILED", HttpStatus.BAD_REQUEST, "파일 업로드에 실패했습니다."),
    INTERNAL_SERVER_ERROR(9999, "SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
            "서버 내부 오류가 발생했습니다.");

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