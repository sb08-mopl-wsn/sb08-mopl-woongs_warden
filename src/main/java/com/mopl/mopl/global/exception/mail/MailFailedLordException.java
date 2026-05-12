package com.mopl.mopl.global.exception.mail;

import com.mopl.mopl.global.exception.BusinessException;
import com.mopl.mopl.global.exception.GlobalErrorCode;

public class MailFailedLordException extends BusinessException {
    public MailFailedLordException() {
        super(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    public MailFailedLordException(String message) {
        super(GlobalErrorCode.INTERNAL_SERVER_ERROR,"메일 생성에 실패했습니다. "+ message);
    }
}
