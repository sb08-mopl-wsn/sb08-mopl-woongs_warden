package com.mopl.mopl.global.exception.mail;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;

public class MailFailedSendException extends BusinessException {
    public MailFailedSendException() {
        super(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    public MailFailedSendException(String message) {
        super(GlobalErrorCode.INTERNAL_SERVER_ERROR,"메일 발송에 실패했습니다. "+ message);
    }
}
