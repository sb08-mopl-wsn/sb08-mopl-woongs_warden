package com.mopl.mopl.infrastructure.ai.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class AiException extends BusinessException
{
    public AiException(AiErrorCode aiErrorCode) {
        super(aiErrorCode);
    }
}
