package com.mopl.mopl.domain.auth.exception;


public class AuthAuthenticationFailedException extends  AuthException{
    public AuthAuthenticationFailedException() {
        super(AuthErrorCode.AUTH_AUTHENTICATION_FAILED);
    }
}