package com.mopl.mopl.domain.content.dto.response;

import com.mopl.mopl.domain.content.entity.ContentType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ContentSummary(
        UUID id,
        ContentType type,
        String title,
        String description,
        String thumbnailUrl,
        List<String> tags,
        BigDecimal averageRating,
        Integer reviewCount
) {
}
