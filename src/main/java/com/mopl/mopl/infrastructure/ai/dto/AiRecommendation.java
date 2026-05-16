package com.mopl.mopl.infrastructure.ai.dto;

import java.util.UUID;

public record AiRecommendation
(
        UUID id,
        String reason
) {}
