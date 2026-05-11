package com.mopl.mopl.infrastructure.external.tmdb.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbTv;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbTvListResponse
(
        int page,
        List<TmdbTv> results,
        int totalPages,
        int totalResults
) {}
