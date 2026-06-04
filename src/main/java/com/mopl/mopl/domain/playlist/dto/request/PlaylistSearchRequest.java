package com.mopl.mopl.domain.playlist.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PlaylistSearchRequest(
    UUID ownerIdEqual,
    UUID subscriberIdEqual,
    String keywordLike,
    @NotNull(message = "limit는 필수입니다.")
    @Min(1)
    @Max(100)
    Integer limit,
    String sortBy,
    String sortDirection,
    String cursor,
    UUID idAfter
) {}