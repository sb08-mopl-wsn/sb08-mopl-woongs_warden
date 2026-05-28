package com.mopl.mopl.infrastructure.elasticsearch.dto;

import java.util.List;
import java.util.UUID;

public record ContentSearchResult
(
        List<UUID> ids,
        long totalHits
) {}
