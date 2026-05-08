package com.mopl.mopl.infrastructure.s3.exception;

public class S3UploadException extends S3Exception
{
    public S3UploadException() {
        super(S3ErrorCode.FILE_UPLOAD_FAILED);
    }
}
