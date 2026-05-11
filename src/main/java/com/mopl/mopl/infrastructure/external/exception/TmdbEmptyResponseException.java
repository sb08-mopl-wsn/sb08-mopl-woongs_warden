package com.mopl.mopl.infrastructure.external.exception;

public class TmdbEmptyResponseException extends ApiException
{
    public TmdbEmptyResponseException() {
        super(ApiErrorCode.TMDB_EMPTY_RESPONSE);
    }
}
