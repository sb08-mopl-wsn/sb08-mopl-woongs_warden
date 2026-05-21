package com.mopl.mopl.infrastructure.ai.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum AiErrorCode implements ErrorCode
{
    AI_SERVICE_UNAVAILABLE(5001, "AI_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스에 연결할 수 없습니다."),
    AI_TIMEOUT(5002, "AI_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT, "AI 응답 시간이 초과되었습니다."),
    AI_PARSE_FAILED(5003, "AI_PARSE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답을 처리할 수 없습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "AI";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}
