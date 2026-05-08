package com.mopl.mopl.infrastructure.s3.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class S3Exception extends BusinessException
{
    public S3Exception(S3ErrorCode errorCode) {
        super(errorCode);
    }
}
