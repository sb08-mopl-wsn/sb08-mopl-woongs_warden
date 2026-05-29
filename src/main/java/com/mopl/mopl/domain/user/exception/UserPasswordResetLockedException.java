package com.mopl.mopl.domain.user.exception;

public class UserPasswordResetLockedException extends UserException {
    public UserPasswordResetLockedException() {
        super(UserErrorCode.USER_PASSWORD_RESET_LOCKED);
    }
}
