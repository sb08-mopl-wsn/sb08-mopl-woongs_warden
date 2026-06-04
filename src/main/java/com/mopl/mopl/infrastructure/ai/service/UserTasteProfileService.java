package com.mopl.mopl.infrastructure.ai.service;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserTasteProfileService
{
    private static final String TASTE_KEY_PREFIX = "teste:";
    private static final Duration TASTE_TTL = Duration.ofHours(1);
    private static final double MIN_RATING = 4.0;
    private static final int TOP_TAGS = 5;

    private final ContentRepository contentRepository;
    private final ReviewRepository reviewRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public float[] getTasteEmbedding(UUID userId) {
        String key = TASTE_KEY_PREFIX + userId;

        // Redis 캐시 조회
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("[취향 프로파일] 캐시 히트: userId={}", userId);
            if (cached instanceof float[]) {
                return (float[]) cached;
            }
            // ArrayList로 역직렬화된 경우 변환
            if (cached instanceof List<?> list) {
                float[] embedding = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    embedding[i] = ((Number) list.get(i)).floatValue();
                }
                return embedding;
            }
        }

        // 리뷰한 콘텐츠들의 embedding 평균 조회
        String avgEmbeddingStr = contentRepository.findAvgEmbeddingByUserId(userId.toString(), MIN_RATING);

        if (avgEmbeddingStr == null) {
            log.debug("[취향 프로파일] 리뷰 없음 (콜드 스타트): userId={}", userId);
            return null;
        }

        float[] avgEmbedding = parseEmbedding(avgEmbeddingStr);

        log.debug("[취향 프로파일] 생성 완료: userId={}", userId);

        // Redis 캐싱
        redisTemplate.opsForValue().set(key, avgEmbedding, TASTE_TTL);

        return avgEmbedding;
    }

    public List<String> getTopTags(UUID userId) {
        List<Content> likedContents = reviewRepository.findHighRatedContents(userId, MIN_RATING);
        if (likedContents.isEmpty()) return List.of();

        return likedContents.stream()
                .flatMap(c -> c.getTags().stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_TAGS)
                .map(Map.Entry::getKey)
                .toList();
    }

    public void evictTasteProfile(UUID userId) {
        String key = TASTE_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("[취향 프로파일] 캐시 무효화: userId={}", userId);
    }

    private float[] parseEmbedding(String embeddingStr) {
        String cleaned = embeddingStr.replaceAll("[\\[\\]\\s]", "");
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }
}
