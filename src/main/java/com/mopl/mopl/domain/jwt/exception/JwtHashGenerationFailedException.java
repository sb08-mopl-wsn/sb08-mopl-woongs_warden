package com.mopl.mopl.domain.jwt.exception;

public class JwtHashGenerationFailedException extends JwtException {
    public JwtHashGenerationFailedException() {
        super(JwtErrorCode.JWT_HASH_GENERATION_FAILED);
    }
}
