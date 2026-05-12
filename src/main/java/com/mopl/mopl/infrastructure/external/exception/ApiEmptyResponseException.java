package com.mopl.mopl.infrastructure.external.exception;

public class ApiEmptyResponseException extends ApiException
{
    public ApiEmptyResponseException() {
        super(ApiErrorCode.TMDB_EMPTY_RESPONSE);
    }
}
