package com.mopl.mopl.domain.user.exception;

import java.util.UUID;

public class UserDuplicateException extends UserException {
    public UserDuplicateException() {
        super(UserErrorCode.USER_DUPLICATE);
    }
}