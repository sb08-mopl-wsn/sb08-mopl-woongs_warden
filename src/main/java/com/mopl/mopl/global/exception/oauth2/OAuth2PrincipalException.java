package com.mopl.mopl.global.exception.oauth2;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;

public class OAuth2PrincipalException extends BusinessException {
    public OAuth2PrincipalException() {
        super(GlobalErrorCode.OAUTH2_PRINVCPAL_NOTMACTH);
    }
}
