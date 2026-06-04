package com.mopl.mopl.infrastructure.ai.service;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContentSimilaritySearchService
{
    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final int CANDIDATE_POOL = 15;
    private static final int CANDIDATE_LIMIT = 10;

    private final ContentRepository contentRepository;

    public List<Content> findSimilarByUserTaste(UUID userId, float[] tasteEmbedding) {
        if (tasteEmbedding == null || tasteEmbedding.length == 0) {
            log.warn("[유사도 검색] 취향 프로파일 없음 (콜드 스타트): userId={}", userId);
            return List.of();
        }

        String embeddingStr = VectorUtils.serialize(tasteEmbedding);
        List<Content> results = contentRepository.findSimilarContents(
                embeddingStr, SIMILARITY_THRESHOLD, CANDIDATE_POOL, CANDIDATE_LIMIT, userId.toString());

        log.debug("[유사도 검색] userId={}, 결과={}건", userId, results.size());
        return results;
    }
}
