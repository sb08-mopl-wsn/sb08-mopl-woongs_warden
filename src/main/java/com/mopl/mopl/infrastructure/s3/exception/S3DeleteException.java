package com.mopl.mopl.infrastructure.s3.exception;

public class S3DeleteException extends S3Exception
{
    public S3DeleteException() {
        super(S3ErrorCode.FILE_DELETE_FAILED);
    }
}
