package com.mopl.mopl.domain.auth.exception;


public class AuthExpiredTokenException extends  AuthException{
    public AuthExpiredTokenException() {
        super(AuthErrorCode.AUTH_EXPIRED_TOKEN);
    }
}