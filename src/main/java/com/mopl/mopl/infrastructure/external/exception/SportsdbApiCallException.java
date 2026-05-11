package com.mopl.mopl.infrastructure.external.exception;

public class SportsdbApiCallException extends ApiException
{
    public SportsdbApiCallException(String season) {
        super(ApiErrorCode.SPORTSDB_API_ERROR, season);
    }
}
