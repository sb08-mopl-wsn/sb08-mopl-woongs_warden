package com.mopl.mopl.infrastructure.ai.strategy;

public record RecommendStrategyContext
(
        RecommendStrategy strategy,
        float[] tasteEmbedding
) {}
