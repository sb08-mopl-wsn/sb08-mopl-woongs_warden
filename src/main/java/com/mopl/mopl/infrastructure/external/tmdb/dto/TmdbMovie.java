package com.mopl.mopl.infrastructure.external.tmdb.dto;

import java.util.List;

public record TmdbMovie
(
        int id,
        String title,
        String overview,
        String posterPath,
        String releaseDate,
        List<Integer> genreIds
) {}
