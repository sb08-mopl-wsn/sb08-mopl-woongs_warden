package com.mopl.mopl.infrastructure.ai.exception;

public class AiUnavailableException extends AiException
{
    public AiUnavailableException() {
        super(AiErrorCode.AI_SERVICE_UNAVAILABLE);
    }
}
