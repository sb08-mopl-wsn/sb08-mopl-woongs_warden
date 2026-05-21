package com.mopl.mopl.infrastructure.ai.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ContentRecommendResponse
(
        UUID id,
        String title,
        String contentType,
        BigDecimal avgRating,
        String thumbnailUrl,
        String reason
) {}
