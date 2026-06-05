package com.mopl.mopl.infrastructure.ai.dto;

import java.util.List;

public record RecommendResult(
        List<ContentRecommendResponse> contents,
        boolean coldStart
) {}
