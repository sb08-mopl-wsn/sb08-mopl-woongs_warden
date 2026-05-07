package com.mopl.mopl.domain.review.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewUpdateRequest(
    String text,
    @Min(value = 0)
    @Max(value = 5)
    Double rating
) {
}
