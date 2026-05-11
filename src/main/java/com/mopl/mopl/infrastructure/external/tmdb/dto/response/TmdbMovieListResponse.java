package com.mopl.mopl.infrastructure.external.tmdb.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbMovie;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbMovieListResponse
(
        int page,
        List<TmdbMovie> results,
        int totalPages,
        int totalResults
) {}
