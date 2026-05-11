package com.mopl.mopl.infrastructure.external.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class ApiException extends BusinessException
{
    public ApiException(ApiErrorCode errorCode) {
        super(errorCode);
    }

    public ApiException(ApiErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
