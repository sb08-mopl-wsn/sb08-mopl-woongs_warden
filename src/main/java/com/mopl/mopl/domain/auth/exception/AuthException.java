package com.mopl.mopl.domain.auth.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class AuthException extends BusinessException {
    public AuthException(AuthErrorCode error) {
        super(error);
    }
    public AuthException(AuthErrorCode error, String message) {
        super(error, message);
    }
}