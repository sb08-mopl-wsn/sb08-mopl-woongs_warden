package com.mopl.mopl.infrastructure.batch.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class BatchException extends BusinessException
{
    public BatchException(BatchErrorCode errorCode) {
        super(errorCode);
    }
}
