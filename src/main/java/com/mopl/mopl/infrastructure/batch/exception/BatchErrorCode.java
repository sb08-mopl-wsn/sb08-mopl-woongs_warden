package com.mopl.mopl.infrastructure.batch.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum BatchErrorCode implements ErrorCode
{
    BATCH_TMDB_COLLECT_FAILED(5001, "BATCH_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "TMDB 콘텐츠 수집 중 오류가 발생했습니다."),
    BATCH_SPORTSDB_COLLECT_FAILED(5002, "BATCH_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "SportsDB 콘텐츠 수집 중 오류가 발생했습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "BATCH";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}
