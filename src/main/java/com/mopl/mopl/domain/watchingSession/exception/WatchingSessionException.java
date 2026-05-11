package com.mopl.mopl.domain.watchingSession.exception;

import com.mopl.mopl.global.exception.BusinessException;

public class WatchingSessionException extends BusinessException {

    public WatchingSessionException(WatchingSessionErrorCode errorCode) {
        super(errorCode);
    }

    public WatchingSessionException(WatchingSessionErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
