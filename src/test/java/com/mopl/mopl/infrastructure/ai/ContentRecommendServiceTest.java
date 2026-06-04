package com.mopl.mopl.infrastructure.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.dto.AiRecommendation;
import com.mopl.mopl.infrastructure.ai.event.SseErrorEvent;
import com.mopl.mopl.infrastructure.ai.recorder.AiPerformanceRecorder;
import com.mopl.mopl.infrastructure.ai.service.ContentRecommendService;
import com.mopl.mopl.infrastructure.ai.service.ContentSimilaritySearchService;
import com.mopl.mopl.infrastructure.ai.service.UserTasteProfileService;
import com.mopl.mopl.infrastructure.ai.strategy.ColdStartRecommendStrategy;
import com.mopl.mopl.infrastructure.ai.strategy.PersonalizedRecommendStrategy;
import com.mopl.mopl.infrastructure.elasticsearch.ContentSearchQueryService;
import com.mopl.mopl.infrastructure.s3.ImageUrlConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@DisplayName("ContentRecommendService Test")
class ContentRecommendServiceTest {

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
    @Mock private AiPerformanceRecorder aiPerformanceRecorder;
    @Mock private Generation generation;
    @Mock private AssistantMessage assistantMessage;
    @Mock private ContentSearchQueryService contentSearchQueryService;
    @Mock private ContentSimilaritySearchService contentSimilaritySearchService;
    @Mock private UserTasteProfileService userTasteProfileService;
    @Mock private PersonalizedRecommendStrategy personalizedStrategy;
    @Mock private ColdStartRecommendStrategy coldStartStrategy;

    private List<Content> contentList;
    private final float[] tasteEmbedding = new float[]{0.1f, 0.2f, 0.3f};

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

        lenient().when(imageUrlConverter.convert(anyString()))
                .thenReturn("https://cdn.example.com/poster.jpg");
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

    @Nested
    @DisplayName("Personalized 추천")
    class Personalized {

        @Test
        @DisplayName("taste profile 있으면 pgvector 기반 추천")
        void givenTasteProfile_whenRecommendStream_thenPersonalizedResult() throws Exception {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            UUID userId = UUID.randomUUID();
            UUID contentId = contentList.getFirst().getId();

            given(userTasteProfileService.getTasteEmbedding(userId)).willReturn(tasteEmbedding);
            given(personalizedStrategy.analyzeIntent(anyString()))
                    .willReturn(new com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis("personalized", List.of(), null));
            given(personalizedStrategy.retrieveCandidates(any(), any(), any()))
                    .willReturn(contentList);

            String jsonResponse = "[{\"id\":\"%s\",\"reason\":\"취향 기반 추천입니다\"}]".formatted(contentId);
            stubLlmCallSuccess(jsonResponse);
            given(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .willReturn(List.of(new AiRecommendation(contentId, "취향 기반 추천입니다")));

            // when
            contentRecommendService.recommendStream("추천해줘", userId, emitter);

            // then
            then(personalizedStrategy).should().retrieveCandidates(any(), eq(userId), eq(tasteEmbedding));
            then(coldStartStrategy).should(never()).retrieveCandidates(any(), any(), any());
            then(emitter).should(atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
            then(emitter).should().complete();
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 ID면 필터링")
        void givenNonExistentContentId_whenRecommendStream_thenFiltersOut() throws Exception {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            UUID userId = UUID.randomUUID();
            UUID fakeId = UUID.randomUUID();

            given(userTasteProfileService.getTasteEmbedding(userId)).willReturn(tasteEmbedding);
            given(personalizedStrategy.analyzeIntent(anyString()))
                    .willReturn(new com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis("personalized", List.of(), null));
            given(personalizedStrategy.retrieveCandidates(any(), any(), any()))
                    .willReturn(contentList);

            String jsonResponse = "[{\"id\":\"%s\",\"reason\":\"존재하지 않는 콘텐츠\"}]".formatted(fakeId);
            stubLlmCallSuccess(jsonResponse);
            given(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .willReturn(List.of(new AiRecommendation(fakeId, "존재하지 않는 콘텐츠")));

            // when
            contentRecommendService.recommendStream("추천해줘", userId, emitter);

            // then
            then(emitter).should(atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
            then(emitter).should().complete();
        }

        @Test
        @DisplayName("LLM 타임아웃 시 AI_TIMEOUT SSE 에러 이벤트 발행")
        void givenLlmTimeout_whenRecommendStream_thenSendsErrorEvent() throws Exception {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            UUID userId = UUID.randomUUID();

            given(userTasteProfileService.getTasteEmbedding(userId)).willReturn(tasteEmbedding);
            given(personalizedStrategy.analyzeIntent(anyString()))
                    .willReturn(new com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis("personalized", List.of(), null));
            given(personalizedStrategy.retrieveCandidates(any(), any(), any()))
                    .willReturn(contentList);

            given(chatClient.prompt()).willReturn(chatClientRequestSpec);
            given(chatClientRequestSpec.system(anyString())).willReturn(chatClientRequestSpec);
            given(chatClientRequestSpec.user(anyString())).willReturn(chatClientRequestSpec);
            given(chatClientRequestSpec.call()).willThrow(new ResourceAccessException("timeout"));

            // when
            contentRecommendService.recommendStream("추천해줘", userId, emitter);

            // then
            ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                    ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
            then(emitter).should(atLeastOnce()).send(captor.capture());

            boolean hasErrorEvent = captor.getAllValues().stream()
                    .anyMatch(builder -> builder.build().stream()
                            .anyMatch(d -> d.getData() instanceof SseErrorEvent errorEvent
                                    && "AI_TIMEOUT".equals(errorEvent.code())));

            assertThat(hasErrorEvent).isTrue();
        }
    }

    @Nested
    @DisplayName("ColdStart 추천")
    class ColdStart {

        @Test
        @DisplayName("taste profile 없으면 인기 콘텐츠 기반 추천")
        void givenNoTasteProfile_whenRecommendStream_thenColdStartResult() throws Exception {
            // given
            SseEmitter emitter = mock(SseEmitter.class);
            UUID userId = UUID.randomUUID();
            UUID contentId = contentList.getFirst().getId();

            given(userTasteProfileService.getTasteEmbedding(userId)).willReturn(null);
            given(coldStartStrategy.analyzeIntent(anyString()))
                    .willReturn(new com.mopl.mopl.infrastructure.ai.dto.IntentAnalysis("trend", List.of(), null));
            given(coldStartStrategy.retrieveCandidates(any(), any(), any()))
                    .willReturn(contentList);

            String jsonResponse = "[{\"id\":\"%s\",\"reason\":\"인기 콘텐츠입니다\"}]".formatted(contentId);
            stubLlmCallSuccess(jsonResponse);
            given(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .willReturn(List.of(new AiRecommendation(contentId, "인기 콘텐츠입니다")));

            // when
            contentRecommendService.recommendStream("추천해줘", userId, emitter);

            // then
            then(coldStartStrategy).should().retrieveCandidates(any(), eq(userId), isNull());
            then(personalizedStrategy).should(never()).retrieveCandidates(any(), any(), any());
            then(emitter).should(atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
            then(emitter).should().complete();
        }
    }
}