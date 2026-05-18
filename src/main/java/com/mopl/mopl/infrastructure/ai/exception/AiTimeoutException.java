package com.mopl.mopl.infrastructure.ai.exception;

public class AiTimeoutException extends AiException
{
    public AiTimeoutException() {
        super(AiErrorCode.AI_TIMEOUT);
    }
}
