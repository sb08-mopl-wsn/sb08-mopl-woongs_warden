package com.mopl.mopl.domain.watchingSession.exception;

import java.util.UUID;

public class WatchingSessionNotFoundException extends WatchingSessionException {

    public WatchingSessionNotFoundException(UUID contentId, UUID userId) {
        super(
                WatchingSessionErrorCode.WatchingSession_NOT_FOUND,
                "현재 시청 중인 세션이 없습니다. contentId: " + contentId + " userId: " + userId
        );
    }
}
