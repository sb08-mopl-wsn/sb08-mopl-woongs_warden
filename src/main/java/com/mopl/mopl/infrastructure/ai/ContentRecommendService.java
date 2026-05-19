package com.mopl.mopl.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.dto.AiRecommendation;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendRequest;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendResponse;
import com.mopl.mopl.infrastructure.ai.exception.AiParseFailedException;
import com.mopl.mopl.infrastructure.ai.exception.AiTimeoutException;
import com.mopl.mopl.infrastructure.ai.exception.AiUnavailableException;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import lombok.RequiredArgsConstructor;
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

    private static final String SYSTEM_PROMPT = """
            너는 MOLE 플랫폼의 콘텐츠 추천 어시스턴트야.
            영화, 드라마, 스포츠 콘텐츠에 대한 질문에만 답변해.
            아래 콘텐츠 목록에서만 추천하고, 목록에 없는 콘텐츠는 절대 추천하지 마.
            반드시 아래 JSON 형식으로만 응답해:
            [{"id": "UUID", "reason": "추천 이유"}]
            최대 5개까지 추천해.
            콘텐츠와 관련 없는 질문에는 빈 배열 []로 응답해.
            """;

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
        // DB 조회
        List<Content> contents = aiPerformanceRecorder.record("db-query", () -> contentRepository.findAll());

        // 프롬프트 조립
        String userMessage = aiPerformanceRecorder.record("prompt-build", () -> {
            String contentContext = buildContentContext(contents);
            return "콘텐츠 목록:\n" + contentContext + "\n\n사용자 질문: " + request.prompt();
        });

        // LLM 호출
        ChatResponse chatResponse = aiPerformanceRecorder.record("llm-call", () -> {
            try {
                return chatClient.prompt()
                        .system(SYSTEM_PROMPT)
                        .user(userMessage)
                        .call()
                        .chatResponse();
            } catch (ResourceAccessException e) {
                throw new AiTimeoutException();
            } catch (NonTransientAiException e) {
                throw new AiUnavailableException();
            }
        });

        // 토큰 사용량 기록
        aiPerformanceRecorder.recordTokenUsage(chatResponse.getMetadata().getUsage());

        // 응답 파싱
        List<AiRecommendation> recommendations = aiPerformanceRecorder.record("response-parse", () -> {
            String rawResponse = chatResponse.getResult().getOutput().getText();

            try {
                return objectMapper.readValue(rawResponse, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new AiParseFailedException();
            }
        });

        // 매핑
        List<AiRecommendation> safeRecommendations = recommendations == null ? List.of() : recommendations;

        Map<UUID, Content> contentMap = contents.stream()
                .collect(Collectors.toMap(Content::getId, content -> content));

        return safeRecommendations.stream()
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
}
