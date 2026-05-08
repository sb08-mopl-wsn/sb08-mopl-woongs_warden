package com.mopl.mopl.infrastructure.s3.exception;

import com.mopl.mopl.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum S3ErrorCode implements ErrorCode
{
    FILE_UPLOAD_FAILED(8001, "UPLOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(8002, "DELETE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다."),
    FILE_INVALID_EXTENSION(8003, "INVALID_EXTENSION", HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(8004, "SIZE_EXCEEDED", HttpStatus.BAD_REQUEST, "파일 크기가 제한을 초과했습니다.");

    private final int numeric;
    private final String errorKey;
    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public String getDomain() {
        return "S3";
    }

    @Override
    public String getCode() {
        return getDomain() + "-" + getErrorKey();
    }
}