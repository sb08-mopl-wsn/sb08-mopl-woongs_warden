package com.mopl.mopl.domain.watchingSession.dto.response;

import com.mopl.mopl.domain.watchingSession.entity.SortDirection;

import java.util.List;
import java.util.UUID;

public record CursorResponseWatchingSessionDto(
        List<WatchingSessionDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        Long totalCount,
        String sortBy,
        SortDirection sortDirection
) {
}
