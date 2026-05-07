package com.mopl.mopl.domain.content.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class ContentException extends BusinessException
{
    public ContentException(ContentErrorCode errorCode) {
        super(errorCode);
    }

    public ContentException(ContentErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
