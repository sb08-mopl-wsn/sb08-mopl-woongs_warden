package com.mopl.mopl.domain.watchingSession.exception;

public class WatchingSessionNotFoundException extends WatchingSessionException {

    public WatchingSessionNotFoundException(String message) {
        super(WatchingSessionErrorCode.WatchingSession_NOT_FOUND);
    }
}
