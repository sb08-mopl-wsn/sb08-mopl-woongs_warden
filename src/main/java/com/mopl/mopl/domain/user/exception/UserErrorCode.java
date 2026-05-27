package com.mopl.mopl.domain.user.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(2001, "NOT_FOUND", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_DUPLICATE(2002, "DUPLICATE", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
    USER_LOGIN_FAILED(2003,"LOGIN_FAILED", HttpStatus.UNAUTHORIZED,"아이디 또는 비밀번호가 일치하지 않습니다."),
    USER_UNAUTHORIZED(2004, "UNAUTHORIZED",HttpStatus.FORBIDDEN,"해당 요청에 대한 권한이 없습니다."),
    USER_INVALID_SOCIAL_INFO(2005, "INVALID_SOCIAL_INFO", HttpStatus.BAD_REQUEST, "socialType과 socialId는 함께 유효해야 합니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "USER";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}
