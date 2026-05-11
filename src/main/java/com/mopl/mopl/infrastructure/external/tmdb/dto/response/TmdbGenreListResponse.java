package com.mopl.mopl.infrastructure.external.tmdb.dto.response;

import java.util.List;

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
