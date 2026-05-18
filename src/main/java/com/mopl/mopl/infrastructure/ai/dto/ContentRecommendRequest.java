package com.mopl.mopl.infrastructure.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContentRecommendRequest
(
        @NotBlank
        @Size(max = 500, message = "프롬프트는 500자 이하여야 합니다.")
        String prompt
) {}
