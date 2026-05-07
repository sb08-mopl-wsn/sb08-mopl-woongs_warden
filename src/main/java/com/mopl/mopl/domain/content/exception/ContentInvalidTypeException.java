package com.mopl.mopl.domain.content.exception;

public class ContentInvalidTypeException extends ContentException
{
    public ContentInvalidTypeException() {
        super(ContentErrorCode.CONTENT_INVALID_TYPE);
    }

    public ContentInvalidTypeException(String type) {
      super(ContentErrorCode.CONTENT_INVALID_TYPE, type);
    }
}
