package com.mopl.mopl.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.dto.AiRecommendation;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendRequest;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendResponse;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;
import com.mopl.mopl.infrastructure.ai.exception.AiParseFailedException;
import com.mopl.mopl.infrastructure.ai.exception.AiTimeoutException;
import com.mopl.mopl.infrastructure.ai.exception.AiUnavailableException;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContentRecommendService
{
    private static final int MAX_RECOMMENDATIONS = 5;

    private final ChatClient chatClient;
    private final ContentRepository contentRepository;
    private final ImageUrlConverter imageUrlConverter;
    private final ObjectMapper objectMapper;
    private final AiPerformanceRecorder aiPerformanceRecorder;
    private final IntentAnalysisService intentAnalysisService;

    /**
     * 사용자의 자연어 질문을 기반으로 AI가 콘텐츠를 추천한다.
     * <p>
     * DB에서 전체 콘텐츠를 조회한 뒤 Gemini API에 프롬프트와 함께 전달하고,
     * AI 응답을 파싱하여 최대 5건의 추천 결과를 반환한다.
     *
     * @param request 사용자의 추천 요청 (자연어 프롬프트)
     * @return 추천 콘텐츠 목록
     * @throws ResourceAccessException Gemini 응답 시간 초과
     * @throws NonTransientAiException Gemini 서비스 불가
     * @throws ConversionFailedException Gemini 응답 파싱 실패
     */
    public List<ContentRecommendResponse> recommend(ContentRecommendRequest request) {
        // 의도 분석
        IntentAnalysis intent = intentAnalysisService.analysis(request.prompt());

        if (intent.intent().equals("unrelated")) {
            log.info("[AI Recommend] 비관련 질문 감지 — 즉시 빈 배열 반환");
            return List.of();
        }

        // 후보 필터링
        List<Content> candidates = findCandidates(intent);

        // 후보 + 의도 정보로 최종 추천
        List<AiRecommendation> recommendations = generateRecommendation(request.prompt(), intent, candidates);

        return mapToResponse(recommendations, candidates);
    }

    /**
     * 의도 분서 결과를 기반으로 후보 콘텐츠를 필터링한다.
     * <p>
     * conetentType과 키워드로 Java 스트림 필터링하며,
     * 후보가 0건이면 전체 콘텐츠로 fallback으로 반환한다.
     *
     * @param intent 의도 분석 결과
     * @return 후보 콘텐츠 목록
     */
    private List<Content> findCandidates(IntentAnalysis intent) {
        return aiPerformanceRecorder.record("candidate-retrieval", () -> {
            List<Content> allContents = contentRepository.findAll();

            List<Content> filtered = allContents.stream()
                    .filter(c -> matchesType(c, intent.contentType()))
                    .filter(c -> matchesKeywords(c, intent.keywords()))
                    .toList();

            log.info("[AI Recommend] 후보 필터링 — 전체 {}건 → 후보 {}건", allContents.size(), filtered.size());

            return filtered.isEmpty() ? allContents : filtered;
        });
    }

    /**
     * 후보 콘텐츠와 사용자 질문을 기반으로 Gemini API를 호출하여 최종 추천을 생성한다.
     *
     * @param prompt     사용자의 원본 질문
     * @param intent     의도 분석 결과
     * @param candidates 후보 콘텐츠 목록
     * @return AI 추천 결과 목록
     * @throws  AiTimeoutException Gemini 응답 시간 초과
     * @throws  AiUnavailableException Gemini 서비스 불가
     * @throws  AiParseFailedException Gemini 응답 파싱 불가
     */
    private List<AiRecommendation> generateRecommendation(String prompt, IntentAnalysis intent, List<Content> candidates) {
        String contentContext = buildContentContext(candidates);
        String userMessage = "사용자 의도: %s\n키워드: %s\n\n후보 콘텐츠 목록:\n%s\n\n사용자 질문: %s"
                .formatted(intent.intent(), intent.keywords(), contentContext, prompt);

        ChatResponse chatResponse = aiPerformanceRecorder.record("final-recommendation", () -> {
            try {
                return chatClient.prompt()
                        .system(AiPrompts.RECOMMENDATION)
                        .user(userMessage)
                        .call()
                        .chatResponse();
            } catch (ResourceAccessException e) {
                throw new AiTimeoutException();
            } catch (NonTransientAiException e) {
                throw new AiUnavailableException();
            }
        });

        aiPerformanceRecorder.recordTokenUsage("final-recommendation", chatResponse.getMetadata().getUsage());

        return aiPerformanceRecorder.record("recommendation-parse", () -> {
            String rawResponse = chatResponse.getResult().getOutput().getText();
            try {
                List<AiRecommendation> result = objectMapper.readValue(
                        rawResponse, new TypeReference<>() {});
                return result == null ? List.of() : result;
            } catch (JsonProcessingException e) {
                throw new AiParseFailedException();
            }
        });
    }

    private List<ContentRecommendResponse> mapToResponse(List<AiRecommendation> recommendations, List<Content> candidates) {
        Map<UUID, Content> contentMap = candidates.stream()
                .collect(Collectors.toMap(Content::getId, content -> content));

        return recommendations.stream()
                .limit(MAX_RECOMMENDATIONS)
                .filter(rec -> contentMap.containsKey(rec.id()))
                .map(rec -> {
                    Content content = contentMap.get(rec.id());
                    return new ContentRecommendResponse(
                            content.getId(),
                            content.getTitle(),
                            content.getContentType().name(),
                            content.getAvgRating(),
                            imageUrlConverter.convert(content.getThumbnailKey()),
                            rec.reason()
                    );
                })
                .toList();
    }

    private String buildContentContext(List<Content> contents) {
        return contents.stream()
                .map(c -> "ID: %s | %s | %s | %s | %s점".formatted(
                        c.getId(), c.getTitle(), c.getContentType(), String.join(",", c.getTags()), c.getAvgRating()
                ))
                .collect(Collectors.joining("\n"));
    }

    private boolean matchesType(Content content, String contentType) {
        if (contentType == null) return true;
        return content.getContentType().name().equals(contentType);
    }

    private boolean matchesKeywords(Content content, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return true;
        return keywords.stream().anyMatch(keyword ->
                content.getTitle().toLowerCase().contains(keyword.toLowerCase())
                        || content.getDescription().toLowerCase().contains(keyword.toLowerCase())
                        || content.getTags().stream().anyMatch(tag ->
                        tag.toLowerCase().contains(keyword.toLowerCase())));
    }
}
