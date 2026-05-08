package com.mopl.mopl.global.event;

import com.mopl.mopl.domain.watchingSession.dto.WatchingSessionChange;

import java.util.UUID;

public record WatchingSessionEvent(
        WatchingSessionChange change,
        UUID contentId
) {
}
