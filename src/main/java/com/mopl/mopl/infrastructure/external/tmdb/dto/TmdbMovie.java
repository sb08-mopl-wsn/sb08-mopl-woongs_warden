package com.mopl.mopl.infrastructure.external.tmdb.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbMovie
(
        int id,
        String title,
        String overview,
        String posterPath,
        String releaseDate,
        List<Integer> genreIds
) {}
