package com.mopl.mopl.domain.user.exception;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;

public class CursorUserException extends BusinessException {
    public CursorUserException() {
        super(GlobalErrorCode.INVALID_INPUT, "cursor와 idAfter는 함께 전달되어야 합니다.");
    }
}