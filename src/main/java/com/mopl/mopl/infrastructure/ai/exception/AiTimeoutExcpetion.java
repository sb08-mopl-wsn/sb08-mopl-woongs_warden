package com.mopl.mopl.infrastructure.ai.exception;

public class AiTimeoutExcpetion extends AiException
{
    public AiTimeoutExcpetion() {
        super(AiErrorCode.AI_TIMEOUT);
    }
}
