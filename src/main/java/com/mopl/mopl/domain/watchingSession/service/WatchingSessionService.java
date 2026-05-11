package com.mopl.mopl.domain.watchingSession.service;

import java.util.UUID;

public interface WatchingSessionService {
    void join(UUID contentId, UUID userId);

    void leave(UUID contentId, UUID userId);
}
