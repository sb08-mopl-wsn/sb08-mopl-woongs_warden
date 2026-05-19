package com.mopl.mopl.domain.auth.exception;

public class AuthFailedRefreshToken extends AuthException {
    public AuthFailedRefreshToken() {
        super(AuthErrorCode.AUTH_AUTHENTICATION_FAILED);
    }
}
