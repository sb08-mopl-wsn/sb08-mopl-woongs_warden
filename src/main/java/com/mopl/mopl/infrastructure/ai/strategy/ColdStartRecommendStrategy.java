package com.mopl.mopl.infrastructure.ai.strategy;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;
import com.mopl.mopl.infrastructure.ai.service.UserTasteProfileService;
import com.mopl.mopl.infrastructure.elasticsearch.ContentSearchQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ColdStartRecommendStrategy implements RecommendStrategy {

    private static final int FALLBACK_LIMIT = 30;

    private final ContentSearchQueryService contentSearchQueryService;
    private final ContentRepository contentRepository;
    private final UserTasteProfileService userTasteProfileService;

    @Override
    public IntentAnalysis analyzeIntent(String prompt) {
        return new IntentAnalysis("trend", List.of(), null);
    }

    @Override
    public List<Content> retrieveCandidates(IntentAnalysis intent, UUID userId, float[] tasteEmbedding) {
        log.info("[AI Recommend] Cold start — 평점 높은 순 추천");
        return contentRepository.findAll(
                PageRequest.of(0, FALLBACK_LIMIT, Sort.by(Sort.Direction.DESC, "avgRating"))
        ).getContent();
    }
}
