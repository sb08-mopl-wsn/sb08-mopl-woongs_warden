package com.mopl.mopl.domain.auth.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    AUTH_INVALID_TOKEN(2501, "INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "유효하지 않는 토큰입니다."),
    AUTH_EXPIRED_TOKEN(2502, "EXPIRED_TOKEN", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    AUTH_REVOKED_TOKEN(2503, "REVOKED_TOKEN", HttpStatus.UNAUTHORIZED, "삭제된 토큰입니다."),
    AUTH_UNAUTHORIZED(2504, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "AUTH";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}