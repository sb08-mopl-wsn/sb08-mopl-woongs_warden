package com.mopl.mopl.global.exception.mail;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;

public class MailFailedLoadException extends BusinessException {
    public MailFailedLoadException() {
        super(GlobalErrorCode.MAIL_LOAD_FAILED);
    }

    public MailFailedLoadException(String message) {
        super(GlobalErrorCode.MAIL_LOAD_FAILED,message);
    }
}
