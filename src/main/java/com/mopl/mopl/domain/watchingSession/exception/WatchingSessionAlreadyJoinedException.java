package com.mopl.mopl.domain.watchingSession.exception;

import java.util.UUID;

public class WatchingSessionAlreadyJoinedException extends WatchingSessionException {

    public WatchingSessionAlreadyJoinedException() {
        super(WatchingSessionErrorCode.WatchingSession_ALREADY_JOIN);
    }

    public WatchingSessionAlreadyJoinedException(UUID contentId, UUID userId) {
        super(WatchingSessionErrorCode.WatchingSession_ALREADY_JOIN,
                "이미 시청 중인 콘텐츠입니다. contentId= " + contentId + " userId= " + userId );
    }
}
