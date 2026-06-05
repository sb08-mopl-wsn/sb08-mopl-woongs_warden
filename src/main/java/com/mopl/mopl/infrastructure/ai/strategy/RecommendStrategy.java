package com.mopl.mopl.infrastructure.ai.strategy;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;

import java.util.List;
import java.util.UUID;

public interface RecommendStrategy
{
    IntentAnalysis analyzeIntent(String prompt);
    List<Content> retrieveCandidates(IntentAnalysis intent, UUID userId, float[] tasteEmbedding);
}
