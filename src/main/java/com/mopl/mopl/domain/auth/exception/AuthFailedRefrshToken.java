package com.mopl.mopl.domain.auth.exception;

public class AuthFailedRefrshToken extends AuthException {
    public AuthFailedRefrshToken() {
        super(AuthErrorCode.AUTH_FAILED_REFRESH_TOEKN);
    }
}
