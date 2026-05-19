package com.mopl.mopl.infrastructure.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.dto.AiRecommendation;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendRequest;
import com.mopl.mopl.infrastructure.ai.dto.ContentRecommendResponse;
import com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis;
import com.mopl.mopl.infrastructure.ai.exception.AiTimeoutException;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentRecommendService Test")
class ContentRecommendServiceTest
{
    @InjectMocks private ContentRecommendService contentRecommendService;

    @Mock private ContentRepository contentRepository;

    @Mock private ChatClient chatClient;
    @Mock private ImageUrlConverter imageUrlConverter;
    @Mock private ChatClientRequestSpec chatClientRequestSpec;
    @Mock private CallResponseSpec callResponseSpec;

    @Mock private ChatResponse chatResponse;
    @Mock private ChatResponseMetadata chatResponseMetadata;
    @Mock private Usage usage;
    @Mock private ObjectMapper objectMapper;
    @Mock private IntentAnalysisService intentAnalysisService;
    @Mock private AiPerformanceRecorder aiPerformanceRecorder;
    @Mock private Generation generation;
    @Mock private AssistantMessage assistantMessage;

    private List<Content> contentList;

    @BeforeEach
    void setUp() {
        Content content1 = Content.builder()
                .title("존 윅 4")
                .description("액션 영화")
                .contentType(ContentType.movie)
                .thumbnailKey("posters/johnwick4.jpg")
                .tags(List.of("action", "thriller"))
                .build();

        Content content2 = Content.builder()
                .title("로맨틱 홀리데이")
                .description("로맨스 영화")
                .contentType(ContentType.movie)
                .thumbnailKey("posters/romantic.jpg")
                .tags(List.of("romance", "comedy"))
                .build();

        ReflectionTestUtils.setField(content1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(content2, "id", UUID.randomUUID());

        contentList = List.of(content1, content2);

        lenient().when(aiPerformanceRecorder.record(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
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
    @DisplayName("장르 기반 추천 성공 — 후보 필터링 후 추천")
    void givenGenreIntent_whenRecommend_thenReturnsFilteredRecommendations() throws Exception {
        // given
        ContentRecommendRequest request = new ContentRecommendRequest("액션 영화 추천해줘");

        given(intentAnalysisService.analysis(anyString()))
                .willReturn(new IntentAnalysis("genre_based", List.of("액션"), "movie"));
        given(contentRepository.findAll()).willReturn(contentList);
        given(imageUrlConverter.convert(anyString())).willReturn("https://cdn.example.com/poster.jpg");

        UUID contentId = contentList.getFirst().getId();
        String jsonResponse = "[{\"id\":\"%s\",\"reason\":\"화려한 액션이 돋보이는 영화입니다\"}]".formatted(contentId);
        stubLlmCallSuccess(jsonResponse);

        List<AiRecommendation> aiResponse = List.of(
                new AiRecommendation(contentId, "화려한 액션이 돋보이는 영화입니다")
        );
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(aiResponse);

        // when
        List<ContentRecommendResponse> result = contentRecommendService.recommend(request);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("존 윅 4");
        assertThat(result.getFirst().reason()).isEqualTo("화려한 액션이 돋보이는 영화입니다");
    }

    @Test
    @DisplayName("비관련 질문이면 즉시 빈 배열 반환 — LLM 2회차 호출 없음")
    void givenUnrelatedIntent_whenRecommend_thenReturnsEmptyImmediately() {
        // given
        ContentRecommendRequest request = new ContentRecommendRequest("오늘 날씨 어때");

        given(intentAnalysisService.analysis(anyString()))
                .willReturn(new IntentAnalysis("unrelated", List.of(), null));

        // when
        List<ContentRecommendResponse> result = contentRecommendService.recommend(request);

        // then
        assertThat(result).isEmpty();
        then(contentRepository).should(never()).findAll();
        then(chatClient).should(never()).prompt();
    }

    @Test
    @DisplayName("후보 0건이면 전체 콘텐츠로 fallback")
    void givenNoCandidates_whenRecommend_thenFallbackToAll() throws Exception {
        // given
        ContentRecommendRequest request = new ContentRecommendRequest("존 윅이랑 비슷한 거");

        given(intentAnalysisService.analysis(anyString()))
                .willReturn(new IntentAnalysis("similar", List.of("매칭안됨"), "movie"));
        given(contentRepository.findAll()).willReturn(contentList);
        given(imageUrlConverter.convert(anyString())).willReturn("https://cdn.example.com/poster.jpg");

        UUID contentId = contentList.getFirst().getId();
        String jsonResponse = "[{\"id\":\"%s\",\"reason\":\"비슷한 액션 영화입니다\"}]".formatted(contentId);
        stubLlmCallSuccess(jsonResponse);

        List<AiRecommendation> aiResponse = List.of(
                new AiRecommendation(contentId, "비슷한 액션 영화입니다")
        );
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(aiResponse);

        // when
        List<ContentRecommendResponse> result = contentRecommendService.recommend(request);

        // then
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("응답에 존재하지 않는 콘텐츠ID면 필터링")
    void givenNonExistentContentId_whenRecommend_thenFiltersOut() throws Exception {
        // given
        ContentRecommendRequest request = new ContentRecommendRequest("액션 영화 추천해줘");

        given(intentAnalysisService.analysis(anyString()))
                .willReturn(new IntentAnalysis("genre_based", List.of("액션"), "movie"));
        given(contentRepository.findAll()).willReturn(contentList);

        UUID fakeId = UUID.randomUUID();
        String jsonResponse = "[{\"id\":\"%s\",\"reason\":\"존재하지 않는 콘텐츠\"}]".formatted(fakeId);
        stubLlmCallSuccess(jsonResponse);

        List<AiRecommendation> aiResponse = List.of(
                new AiRecommendation(fakeId, "존재하지 않는 콘텐츠")
        );
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(aiResponse);

        // when
        List<ContentRecommendResponse> result = contentRecommendService.recommend(request);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Stage 3 타임아웃시 AI TIMEOUT 예외 발생")
    void givenLlmTimeout_whenRecommend_thenThrowsAiTimeout() {
        // given
        ContentRecommendRequest request = new ContentRecommendRequest("액션 영화 추천해줘");

        given(intentAnalysisService.analysis(anyString()))
                .willReturn(new IntentAnalysis("genre_based", List.of("액션"), "movie"));
        given(contentRepository.findAll()).willReturn(contentList);

        given(chatClient.prompt()).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.system(anyString())).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.user(anyString())).willReturn(chatClientRequestSpec);
        given(chatClientRequestSpec.call()).willThrow(new ResourceAccessException("timeout"));

        // when & then
        assertThatThrownBy(() -> contentRecommendService.recommend(request))
                .isInstanceOf(AiTimeoutException.class);
    }
}