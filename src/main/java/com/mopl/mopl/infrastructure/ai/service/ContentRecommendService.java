package com.mopl.mopl.infrastructure.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.infrastructure.ai.dto.AiRecommendation;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendResponse;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;
import com.mopl.mopl.infrastructure.ai.dto.RecommendResult;
import com.mopl.mopl.infrastructure.ai.event.SseErrorEvent;
import com.mopl.mopl.infrastructure.ai.event.SseStatusEvent;
import com.mopl.mopl.infrastructure.ai.exception.AiParseFailedException;
import com.mopl.mopl.infrastructure.ai.exception.AiTimeoutException;
import com.mopl.mopl.infrastructure.ai.exception.AiUnavailableException;
import com.mopl.mopl.infrastructure.ai.prompt.AiPrompts;
import com.mopl.mopl.infrastructure.ai.recorder.AiPerformanceRecorder;
import com.mopl.mopl.infrastructure.ai.strategy.ColdStartRecommendStrategy;
import com.mopl.mopl.infrastructure.ai.strategy.PersonalizedRecommendStrategy;
import com.mopl.mopl.infrastructure.ai.strategy.RecommendStrategyContext;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.retry.NonTransientAiException;
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

    private final ChatClient chatClient;
    private final ImageUrlConverter imageUrlConverter;
    private final ObjectMapper objectMapper;
    private final AiPerformanceRecorder aiPerformanceRecorder;
    private final UserTasteProfileService userTasteProfileService;
    private final PersonalizedRecommendStrategy personalizedStrategy;
    private final ColdStartRecommendStrategy coldStartStrategy;

    @Async(AI_RECOMMEND_EXECUTOR)
    public void recommendStream(String prompt, UUID userId, SseEmitter emitter) {
        try {
            if (!sendStatus(emitter, "candidate_filtering", "맞춤 콘텐츠를 찾고 있습니다...")) return;

            RecommendStrategyContext context = resolveStrategy(userId);
            boolean isColdStart = context.strategy() instanceof ColdStartRecommendStrategy;
            IntentAnalysis intent = context.strategy().analyzeIntent(prompt);
            List<Content> candidates = context.strategy()
                    .retrieveCandidates(intent, userId, context.tasteEmbedding());

            if (!sendStatus(emitter, "recommendation", "추천 결과를 생성하고 있습니다...")) return;
            List<AiRecommendation> recommendations = generateRecommendation(prompt, intent, candidates);
            sendResult(emitter, new RecommendResult(mapToResponse(recommendations, candidates), isColdStart));
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

    private RecommendStrategyContext resolveStrategy(UUID userId) {
        if (userId == null) {
            return new RecommendStrategyContext(coldStartStrategy, null);
        }
        float[] tasteEmbedding = userTasteProfileService.getTasteEmbedding(userId);
        if (tasteEmbedding != null) {
            return new RecommendStrategyContext(personalizedStrategy, tasteEmbedding);
        }
        return new RecommendStrategyContext(coldStartStrategy, null);
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

    private void sendResult(SseEmitter emitter, RecommendResult result) {
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
