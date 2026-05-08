package com.mopl.mopl.domain.user.exception;

public class UserUnauthorizedException extends UserException {
    public UserUnauthorizedException() {
        super(UserErrorCode.USER_UNAUTHORIZED);
    }
}