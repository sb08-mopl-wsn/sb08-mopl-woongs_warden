package com.mopl.mopl.infrastructure.external.tmdb.dto.response;

import com.mopl.mopl.infrastructure.external.tmdb.dto.TmdbTv;

import java.util.List;

public record TmdbTvListResponse
(
        int page,
        List<TmdbTv> results,
        int totalPages,
        int totalResults
) {}
