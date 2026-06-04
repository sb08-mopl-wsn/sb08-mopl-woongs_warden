package com.mopl.mopl.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;
import com.mopl.mopl.infrastructure.ai.exception.AiTimeoutException;
import com.mopl.mopl.infrastructure.ai.recorder.AiPerformanceRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntentAnalysisService Test")
class IntentAnalysisServiceTest
{
    @InjectMocks private IntentAnalysisService intentAnalysisService;

    @Mock private ChatClient chatClient;
    @Mock private ObjectMapper objectMapper;
    @Mock private AiPerformanceRecorder recorder;
    @Mock private ChatClientRequestSpec chatClientRequestSpec;
    @Mock private CallResponseSpec callResponseSpec;
    @Mock private ChatResponse chatResponse;
    @Mock private ChatResponseMetadata chatResponseMetadata;
    @Mock private Usage usage;
    @Mock private Generation generation;
    @Mock private AssistantMessage assistantMessage;

    @BeforeEach
    void setUp() {
        given(recorder.record(anyString(), any(Supplier.class)))
                .willAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
    }

    private void stubLlmCallSuccess(String jsonResponse) {
        given(chatClient.prompt()).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.system(anyString())).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.user(anyString())).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.chatResponse()).willReturn(chatResponse);
        lenient().when(chatResponse.getMetadata()).thenReturn(chatResponseMetadata);
        lenient().when(chatResponseMetadata.getUsage()).thenReturn(usage);
        lenient().when(usage.getPromptTokens()).thenReturn(50);
        lenient().when(usage.getCompletionTokens()).thenReturn(20);
        given(chatResponse.getResult()).willReturn(generation);
        given(generation.getOutput()).willReturn(assistantMessage);
        given(assistantMessage.getText()).willReturn(jsonResponse);
    }

    @Test
    @DisplayName("장르 기반 질문 의도 분석 성공")
    void givenGenrePrompt_whenAnalyze_thenReturnsGenreIntent() throws Exception {
        // given
        String jsonResponse = "{\"intent\":\"genre_based\",\"keywords\":[\"액션\"],\"contentType\":\"movie\"}";
        stubLlmCallSuccess(jsonResponse);

        IntentAnalysis expected = new IntentAnalysis("genre_based", List.of("액션"), "movie");
        given(objectMapper.readValue(eq(jsonResponse), eq(IntentAnalysis.class))).willReturn(expected);

        // when
        IntentAnalysis result = intentAnalysisService.analysis("액션 영화 추천해줘");

        // then
        assertThat(result.intent()).isEqualTo("genre_based");
        assertThat(result.keywords()).containsExactly("액션");
        assertThat(result.contentType()).isEqualTo("movie");
    }

    @Test
    @DisplayName("비관련 질문 의도 분석")
    void givenUnrelatedPrompt_whenAnalyze_thenReturnsUnrelated() throws Exception {
        // given
        String jsonResponse = "{\"intent\":\"unrelated\",\"keywords\":[],\"contentType\":null}";
        stubLlmCallSuccess(jsonResponse);

        IntentAnalysis expected = new IntentAnalysis("unrelated", List.of(), null);
        given(objectMapper.readValue(eq(jsonResponse), eq(IntentAnalysis.class))).willReturn(expected);

        // when
        IntentAnalysis result = intentAnalysisService.analysis("오늘 날씨 어때");

        // then
        assertThat(result.intent()).isEqualTo("unrelated");
        assertThat(result.keywords()).isEmpty();
        assertThat(result.contentType()).isNull();
    }

    @Test
    @DisplayName("타임아웃시 AI TIMEOUT 예외 발생")
    void givenTimeout_whenAnalyze_thenThrowsAiTimeout() {
        // given
        given(chatClient.prompt()).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.system(anyString())).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.user(anyString())).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.call()).willThrow(new ResourceAccessException("timeout"));

        // when & then
        assertThatThrownBy(() -> intentAnalysisService.analysis("액션 영화 추천해줘"))
                .isInstanceOf(AiTimeoutException.class);
    }
}