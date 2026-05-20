package com.mopl.mopl.global.exception.oauth2;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;

public class OAuth2FailedTokenException extends BusinessException {
    public OAuth2FailedTokenException() {
        super(GlobalErrorCode.OAUTH2_TOKEN_GENERATION_FAILED);
    }


    public OAuth2FailedTokenException(String message) {
        super(GlobalErrorCode.OAUTH2_TOKEN_GENERATION_FAILED,message);
    }
}
