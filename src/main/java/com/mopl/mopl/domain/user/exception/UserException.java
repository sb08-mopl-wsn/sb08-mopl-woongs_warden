package com.mopl.mopl.domain.user.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class UserException extends BusinessException {
    public UserException(UserErrorCode error) {
        super(error);
    }
    public UserException(UserErrorCode error, String message) {
        super(error, message);
    }
}