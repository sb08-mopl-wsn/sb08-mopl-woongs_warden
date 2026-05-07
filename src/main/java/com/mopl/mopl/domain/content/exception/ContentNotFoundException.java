package com.mopl.mopl.domain.content.exception;

import java.util.UUID;

public class ContentNotFoundException extends ContentException
{
    public ContentNotFoundException() {
        super(ContentErrorCode.CONTENT_NOT_FOUND);
    }

    public ContentNotFoundException(UUID contentId) {
        super(ContentErrorCode.CONTENT_NOT_FOUND,
                "콘텐츠를 찾을 수 없습니다. id=" + contentId);
    }
}
