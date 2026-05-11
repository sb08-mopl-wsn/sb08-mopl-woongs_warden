package com.mopl.mopl.infrastructure.external.tmdb.dto;

import java.util.List;

public record TmdbTv
(
        int id,
        String name,
        String overview,
        String posterPath,
        String firstAirDate,
        List<Integer> genreIds
) {}
