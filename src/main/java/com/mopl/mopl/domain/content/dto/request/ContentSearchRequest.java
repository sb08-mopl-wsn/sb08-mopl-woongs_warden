package com.mopl.mopl.domain.content.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

public record ContentSearchRequest
(
        @RequestParam(required = false) String typeEqual,
        @RequestParam(required = false) String keywordLike,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) UUID idAfter,
        @RequestParam @Positive @Max(100) int limit,
        @RequestParam String sortDirection,
        @RequestParam String sortBy
) {}
