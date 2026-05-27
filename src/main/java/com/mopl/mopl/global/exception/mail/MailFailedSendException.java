package com.mopl.mopl.global.exception.mail;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;

public class MailFailedSendException extends BusinessException {
    public MailFailedSendException() {
        super(GlobalErrorCode.MAIL_SEND_FAILED);
    }

    public MailFailedSendException(String message) {
        super(GlobalErrorCode.MAIL_SEND_FAILED, message);
    }
}
