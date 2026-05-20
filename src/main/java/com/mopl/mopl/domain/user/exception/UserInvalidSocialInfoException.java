package com.mopl.mopl.domain.user.exception;

public class UserInvalidSocialInfoException extends UserException {
    public UserInvalidSocialInfoException() {
        super(UserErrorCode.USER_INVALID_SOCIAL_INFO);
    }
}
