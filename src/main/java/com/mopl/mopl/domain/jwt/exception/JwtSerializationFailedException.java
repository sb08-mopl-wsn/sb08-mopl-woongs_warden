package com.mopl.mopl.domain.jwt.exception;

public class JwtSerializationFailedException extends JwtException {
    public JwtSerializationFailedException() {
        super(JwtErrorCode.JWT_SERIALIZATION_FAILED);
    }
}
