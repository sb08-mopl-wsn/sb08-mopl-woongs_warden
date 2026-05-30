package com.mopl.mopl.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.dto.AiRecommendation;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendResponse;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;
import com.mopl.mopl.infrastructure.ai.event.SseErrorEvent;
import com.mopl.mopl.infrastructure.ai.event.SseStatusEvent;
import com.mopl.mopl.infrastructure.ai.exception.AiParseFailedException;
import com.mopl.mopl.infrastructure.ai.exception.AiTimeoutException;
import com.mopl.mopl.infrastructure.ai.exception.AiUnavailableException;
import com.mopl.mopl.infrastructure.elasticsearch.ContentSearchQueryService;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.mopl.mopl.global.config.AsyncConfig.AI_RECOMMEND_EXECUTOR;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContentRecommendService
{
    private static final int MAX_RECOMMENDATIONS = 5;
    private static final int FALLBACK_LIMIT = 50;

    private final ChatClient chatClient;
    private final ContentRepository contentRepository;
    private final ImageUrlConverter imageUrlConverter;
    private final ObjectMapper objectMapper;
    private final AiPerformanceRecorder aiPerformanceRecorder;
    private final IntentAnalysisService intentAnalysisService;
    private final ContentSearchQueryService contentSearchQueryService;
    private final ContentSimilaritySearchService contentSimilaritySearchService;
    private final UserTasteProfileService userTasteProfileService;

    @Async(AI_RECOMMEND_EXECUTOR)
    public void recommendStream(String prompt, UUID userId, SseEmitter emitter) {
        try {
            // Stage 1: 의도 분석
            if (!sendStatus(emitter, "intent_analysis", "질문을 분석하고 있습니다...")) return;
            IntentAnalysis intentAnalysis = intentAnalysisService.analysis(prompt);

            // 비관련 질문 처리
            if ("unrelated".equals(intentAnalysis.intent())) {
                log.info("[AI Recommend] 비관련 질문 감지 - 즉시 빈 배열 반환");
                sendResult(emitter, List.of());
                emitter.complete();
                return;
            }

            // Stage 2: 후보 필터링
            if (!sendStatus(emitter, "candidate_filtering", "맞춤 콘텐츠를 찾고 있습니다...")) return;
            List<Content> candidates = findCandidates(intentAnalysis, userId);

            // Stage 3: 텍스트 스트리밍 추천
            if (!sendStatus(emitter, "recommendation", "추천 결과를 생성하고 있습니다...")) return;
            List<AiRecommendation> recommendations = generateRecommendation(prompt, intentAnalysis, candidates);

            List<ContentRecommendResponse> response = mapToResponse(recommendations, candidates);
            sendResult(emitter, response);
            emitter.complete();
        } catch (AiTimeoutException e) {
            sendError(emitter, "AI_TIMEOUT", "AI 서비스 응답이 지연되고 있습니다.");
        } catch (AiUnavailableException e) {
            sendError(emitter, "AI_UNAVAILABLE", "AI 서비스를 일시적으로 사용할 수 없습니다.");
        } catch (AiParseFailedException e) {
            sendError(emitter, "AI_PARSE_FAILED", "AI 응답을 처리하지 못했습니다.");
        } catch (Exception e) {
            log.error("[AI Recommend] SSE 스트리밍 중 예외 발생", e);
            sendError(emitter, "INTERNAL_ERROR", "추천 처리 중 오류가 발생했습니다.");
        }
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
    private List<Content> findCandidates(IntentAnalysis intent, UUID userId) {
        return aiPerformanceRecorder.record("candidate-retrieval", () -> {
            List<Content> similarContents = contentSimilaritySearchService.findSimilarByUserTaste(userId);

            if (!similarContents.isEmpty()) {
                log.info("[AI Recommend] 취향 기반 후보 {}건", similarContents.size());
                return similarContents;
            }

            List<String> keywords = intent.keywords();
            if ((keywords == null || keywords.isEmpty()) && userId != null) {
                keywords = userTasteProfileService.getTopTags(userId);
                log.info("[AI Recommend] 키워드 없음 — 취향 태그로 대체: {}", keywords);
            }

            List<String> candidateIds = contentSearchQueryService.searchCandidateIds(intent.contentType(), keywords);

            if (candidateIds.isEmpty()) {
                log.warn("[AI Recommend] ES 후보 0건 — 상위 {}건으로 fallback", FALLBACK_LIMIT);
                return contentRepository.findAll(PageRequest.of(0, FALLBACK_LIMIT)).getContent();
            }

            List<UUID> uuids = candidateIds.stream().map(UUID::fromString).toList();
            List<Content> candidates = contentRepository.findAllById(uuids);

            log.info("[AI Recommend] 후보 필터링 — ES {}건 → DB 조회 {}건", candidateIds.size(), candidates.size());

            return candidates;
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
        log.debug("[AI Recommend] userMessage: {}", userMessage);

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
            log.debug("[AI Recommend] raw response: {}", rawResponse);
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

    private boolean sendStatus(SseEmitter emitter, String stage, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data(new SseStatusEvent(stage, message)));
            return true;
        } catch (IOException e) {
            log.warn("[AI Recommend] SSE status 전송 실패 — 클라이언트 연결 끊김");
            return false;
        }
    }

    private void sendResult(SseEmitter emitter, List<ContentRecommendResponse> result) {
        try {
            emitter.send(SseEmitter.event()
                    .name("result")
                    .data(result));
        } catch (IOException e) {
            log.warn("[AI Recommend] SSE result 전송 실패 — 클라이언트 연결 끊김");
        }
    }

    private void sendError(SseEmitter emitter, String code, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new SseErrorEvent(code, message)));
            emitter.complete();
        } catch (IOException e) {
            log.warn("[AI Recommend] SSE error 전송 실패 — 클라이언트 연결 끊김");
        }
    }
}
