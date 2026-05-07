package com.mopl.mopl.domain.content.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContentErrorCode implements ErrorCode {

    CONTENT_NOT_FOUND(1001, "NOT_FOUND", HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."),
    CONTENT_DUPLICATE(1002, "DUPLICATE", HttpStatus.CONFLICT, "이미 존재하는 콘텐츠입니다."),
    CONTENT_INVALID_TYPE(1003, "INVALID_TYPE", HttpStatus.BAD_REQUEST, "유효하지 않은 콘텐츠 유형입니다."),
    CONTENT_INVALID_CURSOR(1004, "INVALID_CURSOR", HttpStatus.BAD_REQUEST, "유효하지 않은 커서 값입니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "CONT";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}
