package com.mopl.mopl.domain.playlist.dto.response;

import com.mopl.mopl.domain.content.dto.response.ContentSummary;
import com.mopl.mopl.domain.user.dto.UserSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlaylistDto(
    UUID id,
    UserSummary owner,
    String title,
    String description,
    Instant updatedAt,
    Long subscriberCount,
    Boolean subscribedByMe,
    List<ContentSummary> contents
) {
}