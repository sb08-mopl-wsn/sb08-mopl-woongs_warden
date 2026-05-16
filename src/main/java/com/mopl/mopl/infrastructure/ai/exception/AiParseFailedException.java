package com.mopl.mopl.infrastructure.ai.exception;

public class AiParseFailedException extends AiException
{
    public AiParseFailedException() {
        super(AiErrorCode.AI_PARSE_FAILED);
    }
}
