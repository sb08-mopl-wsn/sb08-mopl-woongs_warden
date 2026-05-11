package com.mopl.mopl.infrastructure.external.tmdb.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbGenreListResponse
(
        List<Genre> genres
) {
    public record Genre
    (
            int id,
            String name
    ) {}
}
