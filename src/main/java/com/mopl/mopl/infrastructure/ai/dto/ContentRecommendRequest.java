package com.mopl.mopl.infrastructure.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record ContentRecommendRequest
(
        @NotBlank String prompt
) {}
