package com.mopl.mopl.domain.user.exception;

public class UserDuplicateException extends UserException {
    public UserDuplicateException() {
        super(UserErrorCode.USER_DUPLICATE);
    }
}