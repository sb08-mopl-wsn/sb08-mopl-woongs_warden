package com.mopl.mopl.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;

@ParameterObject
public record ReviewSearchRequest(
    @NotNull(message = "콘텐츠 ID는 필수입니다.")
    UUID contentId,
    @Min(1)
    @Max(100)
    Integer limit,
    String sortBy,
    String sortDirection,
    String cursor,
    UUID idAfter
) {}