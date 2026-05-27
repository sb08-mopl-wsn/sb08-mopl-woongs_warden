package com.mopl.mopl.domain.auth.exception;


public class AuthInvalidTokenException extends  AuthException{
    public AuthInvalidTokenException() {
        super(AuthErrorCode.AUTH_INVALID_TOKEN);
    }
}