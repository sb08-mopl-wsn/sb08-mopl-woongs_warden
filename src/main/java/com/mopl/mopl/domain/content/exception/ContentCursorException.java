package com.mopl.mopl.domain.content.exception;

public class ContentCursorException extends ContentException
{
    public ContentCursorException() {
        super(ContentErrorCode.CONTENT_INVALID_CURSOR);
    }
}
