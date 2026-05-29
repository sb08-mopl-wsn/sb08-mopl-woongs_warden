package com.mopl.mopl.domain.jwt.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JwtErrorCode implements ErrorCode {
    JWT_SERIALIZATION_FAILED(2601, "SERIALIZATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
            "JWT 정보를 Redis에 저장하는 중 오류가 발생했습니다."),
    JWT_HASH_GENERATION_FAILED(2602, "HASH_GENERATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
            "JWT 토큰 해시 생성 중 오류가 발생했습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "JWT";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}
