package com.mopl.mopl.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;
import com.mopl.mopl.infrastructure.ai.exception.AiTimeoutException;
import com.mopl.mopl.infrastructure.ai.exception.AiUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class IntentAnalysisService
{
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AiPerformanceRecorder aiPerformanceRecorder;

    /**
     * 사용자의 자연어 질문을 분석하여 의도, 키워드, 콘텐츠 타입을 추출한다.
     * <p>
     * Gemini API를 호출하여 Structured Output으로 응답을 수신하며,
     * 파싱 실패 시 genre_based 의도로 fallback 처리한다.
     *
     * @param prompt 사용자의 자연어 질문
     * @return 의도 분석 결과
     * @throws AiTimeoutException Gemini 응답 시간 초과
     * @throws AiUnavailableException Gemini 서비스 불가
     */
    public IntentAnalysis analysis(String prompt) {
        ChatResponse chatResponse = aiPerformanceRecorder.record("intent-classification", () -> {
            try {
                return chatClient.prompt()
                        .system(AiPrompts.INTENT_ANALYSIS)
                        .user(prompt)
                        .call()
                        .chatResponse();
            } catch (ResourceAccessException e) {
                throw  new AiTimeoutException();
            } catch (NonTransientAiException e) {
                throw new AiUnavailableException();
            }
        });

        return aiPerformanceRecorder.record("intent-parse", () -> {
            String rawResponse = chatResponse.getResult().getOutput().getText();

            try {
                return objectMapper.readValue(rawResponse, IntentAnalysis.class);
            } catch (JsonProcessingException e) {
                log.warn("[AI Recommend] 의도 분석 파싱 실패 — fallback 적용: {}", rawResponse);

                return new IntentAnalysis("genre_based", List.of(), null);
            }
        });
    }
}
