package com.mopl.mopl.infrastructure.ai.strategy;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;
import com.mopl.mopl.infrastructure.ai.service.ContentSimilaritySearchService;
import com.mopl.mopl.infrastructure.ai.service.UserTasteProfileService;
import com.mopl.mopl.infrastructure.elasticsearch.ContentSearchQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class PersonalizedRecommendStrategy implements RecommendStrategy
{
    private static final int FALLBACK_LIMIT = 50;

    private final ContentSimilaritySearchService contentSimilaritySearchService;
    private final ContentSearchQueryService contentSearchQueryService;
    private final ContentRepository contentRepository;
    private final UserTasteProfileService userTasteProfileService;

    @Override
    public IntentAnalysis analyzeIntent(String prompt) {
        return new IntentAnalysis("personalized", List.of(), null);
    }

    @Override
    public List<Content> retrieveCandidates(IntentAnalysis intent, UUID userId, float[] tasteEmbedding) {
        List<Content> candidates = contentSimilaritySearchService
                .findSimilarByUserTaste(userId, tasteEmbedding);

        if (!candidates.isEmpty()) return candidates;

        log.info("[AI Recommend] pgvector 후보 없음 — 취향 태그로 OpenSearch fallback");
        List<String> topTags = userTasteProfileService.getTopTags(userId);
        List<String> candidateIds = contentSearchQueryService.searchCandidateIds(null, topTags);
        List<UUID> uuids = candidateIds.stream().map(UUID::fromString).toList();

        return uuids.isEmpty()
                ? contentRepository.findAll(PageRequest.of(0, FALLBACK_LIMIT)).getContent()
                : contentRepository.findAllById(uuids);
    }
}
