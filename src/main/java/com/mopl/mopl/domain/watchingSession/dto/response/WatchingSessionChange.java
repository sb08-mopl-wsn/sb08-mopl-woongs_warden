package com.mopl.mopl.domain.watchingSession.dto.response;

import com.mopl.mopl.domain.watchingSession.entity.ChangeType;

public record WatchingSessionChange(
        // LEAVE / JOIN
        ChangeType type,
        // 시청 세션 정보
        WatchingSessionDto watchingSession,
        // 시청자 수
        long watcherCount
) {
}
