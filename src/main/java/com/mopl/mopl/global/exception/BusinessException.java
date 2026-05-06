package com.mopl.mopl.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private String hint;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    // 메시지를 직접 지정하고 싶을 때
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    // 힌트를 포함하고 싶을 때
    public BusinessException(ErrorCode errorCode, String message, String hint) {
        super(message);
        this.errorCode = errorCode;
        this.hint = hint;
    }
}
