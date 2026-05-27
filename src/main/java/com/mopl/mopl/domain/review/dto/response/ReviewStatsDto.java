package com.mopl.mopl.domain.review.dto.response;

public record ReviewStatsDto(
    Long reviewCount,
    Double averageRating
) {}