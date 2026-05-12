package com.mopl.mopl.infrastructure.external.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ApiErrorCode implements ErrorCode
{
    TMDB_EMPTY_RESPONSE(8501,"TMDB_EMPTY_RESPONSE", HttpStatus.BAD_GATEWAY, "TMDB 응답 본문이 비어 있습니다."),
    SPORTSDB_API_ERROR(8502,"SPORTSDB_API_ERROR", HttpStatus.BAD_GATEWAY, "SportsDB API 호출에 실패했습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "API";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}
