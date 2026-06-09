package com.mopl.mopl.domain.content.dto.response;

import java.util.List;
import java.util.UUID;

public record CursorResponseContentDto
(
        List<ContentDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
) {}
