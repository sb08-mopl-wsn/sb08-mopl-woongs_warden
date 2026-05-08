package com.mopl.mopl.domain.watchingSession.dto;

import com.mopl.mopl.domain.watchingSession.entity.ChangeType;

public record WatchingSessionChange(
        ChangeType type,
        WatchingSessionDto watchingSession,
        long watcherCount
) {
}
