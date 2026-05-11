package com.mopl.mopl.infrastructure.external.tmdb.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TmdbTv
(
        int id,
        String name,
        String overview,
        String posterPath,
        String firstAirDate,
        List<Integer> genreIds
) {}
