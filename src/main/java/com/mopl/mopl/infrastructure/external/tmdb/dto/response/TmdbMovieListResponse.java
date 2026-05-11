package com.mopl.mopl.infrastructure.external.tmdb.dto.response;

import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbMovie;

import java.util.List;

public record TmdbMovieListResponse
(
        int page,
        List<TmdbMovie> results,
        int totalPages,
        int totalResults
) {}
