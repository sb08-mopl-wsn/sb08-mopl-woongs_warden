package com.mopl.mopl.domain.watchingSession.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WatchingSessionErrorCode implements ErrorCode {

    WatchingSession_NOT_FOUND(7001, "NOT_FOUND", HttpStatus.NOT_FOUND ,"현재 시청 중인 세션이 없습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "WATCH";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}
