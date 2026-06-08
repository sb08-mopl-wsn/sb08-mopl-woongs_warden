package com.mopl.mopl.domain.content.dto.response;

import com.mopl.mopl.domain.content.entity.ContentType;

import java.util.List;
import java.util.UUID;

public record ContentDto
(
        UUID id,
        String title,
        String description,
        ContentType type,
        String thumbnailUrl,
        List<String> tags,
        double averageRating,
        int reviewCount,
        int watcherCount
) {
    public ContentDto withWatcherCount(int watcherCount) {
        return new ContentDto(id, title, description, type, thumbnailUrl, tags, averageRating, reviewCount, watcherCount);
    }
}
