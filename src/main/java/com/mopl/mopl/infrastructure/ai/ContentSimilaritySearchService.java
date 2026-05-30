package com.mopl.mopl.infrastructure.ai;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
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
    private static final int SIMILAR_CONTENT_LIMIT = 50;

    private final ContentRepository contentRepository;
    private final UserTasteProfileService userTasteProfileService;

    public List<Content> findSimilarByUserTaste(UUID userId) {
        float[] tasteEmbedding = userTasteProfileService.getTasteEmbedding(userId);

        if (tasteEmbedding == null || tasteEmbedding.length == 0) {
            log.warn("[유사도 검색] 취향 프로파일 없음 (콜드 스타트): userId={}", userId);
            return List.of();
        }

        String embeddingStr = toVectorString(tasteEmbedding);
        List<Content> results = contentRepository.findSimilarContents(embeddingStr, SIMILAR_CONTENT_LIMIT, userId.toString());

        log.debug("[유사도 검색] userId={}, 결과={}건", userId, results.size());
        return results;
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
