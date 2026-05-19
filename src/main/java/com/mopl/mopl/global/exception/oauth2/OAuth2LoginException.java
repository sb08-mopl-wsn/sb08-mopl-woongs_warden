package com.mopl.mopl.global.exception.oauth2;

import com.mopl.mopl.global.exception.GlobalErrorCode;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class OAuth2LoginException extends OAuth2AuthenticationException {

    public OAuth2LoginException(GlobalErrorCode errorCode) {
        super(
                new OAuth2Error(errorCode.getCode()),
                errorCode.getMessage()
        );
    }

    public OAuth2LoginException(GlobalErrorCode errorCode, String detail) {
        super(
                new OAuth2Error(errorCode.getCode()),
                errorCode.getMessage() + " " + detail
        );
    }
}