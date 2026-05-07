package com.mopl.mopl.domain.user.exception;

import java.util.UUID;

public class UserLoginFailedException extends UserException {
    public UserLoginFailedException() {
        super(UserErrorCode.USER_LOGIN_FAILED);
    }
}