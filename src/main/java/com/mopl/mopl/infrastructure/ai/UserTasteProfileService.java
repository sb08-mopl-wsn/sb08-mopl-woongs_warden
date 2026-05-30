package com.mopl.mopl.infrastructure.ai;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
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

    private final EmbeddingModel embeddingModel;
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

        // 평점 4점 이상 콘텐츠 조회
        List<Content> likedContents = reviewRepository.findHighRatedContents(userId, MIN_RATING);

        if (likedContents.isEmpty()) {
            log.debug("[취향 프로파일] 리뷰 없음 (콜드 스타트): userId={}", userId);
            return null;
        }

        // 태그 집계 -> 상위 5개 추출
        // 1. 모든 태그 추출
        List<String> allTags = likedContents.stream()
                .flatMap(c -> c.getTags().stream())
                .toList();

        // 2. 태그 빈도 집계
        Map<String, Long> tagCount = allTags.stream()
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 3. 상위 5개 태그 추출
        String tasteText = tagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_TAGS)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(" "));

        log.debug("[취향 프로파일] 생성: userId={}, tags={}", userId, tasteText);

        // 취향 텍스트 → 임베딩 벡터
        float[] embedding = embeddingModel.embed(tasteText);

        // Redis 캐싱
        redisTemplate.opsForValue().set(key, embedding, TASTE_TTL);

        return embedding;
    }

    public List<String> getTopTags(UUID userId) {
        List<Content> likedContents = reviewRepository.findHighRatedContents(userId, MIN_RATING);
        if (likedContents.isEmpty()) return List.of();

        List<String> allTags = likedContents.stream()
                .flatMap(c -> c.getTags().stream())
                .toList();

        return allTags.stream()
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_TAGS)
                .map(Map.Entry::getKey)
                .toList();
    }
}
