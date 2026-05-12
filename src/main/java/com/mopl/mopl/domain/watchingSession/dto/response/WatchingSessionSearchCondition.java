package com.mopl.mopl.domain.watchingSession.dto.response;

import com.mopl.mopl.domain.watchingSession.entity.SortDirection;

import java.util.UUID;

// 필터 조건을 위한 DTO
public record WatchingSessionSearchCondition(
        UUID contentId,
        String cursor,
        String username,
        UUID idAfter,
        SortDirection sortDirection
) {
}
