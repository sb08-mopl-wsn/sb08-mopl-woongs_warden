package com.mopl.mopl.domain.auth.exception;


public class AuthRevokedTokenException extends  AuthException{
    public AuthRevokedTokenException() {
        super(AuthErrorCode.AUTH_REVOKED_TOKEN);
    }
}