package com.mopl.mopl.global.exception.oauth2;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;

public class Oauth2FailedTokenException extends BusinessException {
    public Oauth2FailedTokenException() {
        super(GlobalErrorCode.OAUTH2_TOKEN_GENERATION_FAILED);
    }


    public Oauth2FailedTokenException(String message) {
        super(GlobalErrorCode.OAUTH2_TOKEN_GENERATION_FAILED,message);
    }
}
