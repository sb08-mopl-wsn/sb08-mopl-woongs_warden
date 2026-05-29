package com.mopl.mopl.domain.jwt.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class JwtException extends BusinessException {
    public JwtException(JwtErrorCode error) {
        super(error);
    }

    public JwtException(JwtErrorCode error, String message) {
        super(error, message);
    }
}
