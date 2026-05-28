package com.mopl.mopl.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.elasticsearch.ContentIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentIndexConsumer Test")
class ContentIndexConsumerTest
{
    @InjectMocks private ContentIndexConsumer contentIndexConsumer;
    @Mock private ContentIndexService contentIndexService;
    @Mock private ContentRepository contentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contentIndexConsumer, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("INDEX 액션 - 콘텐츠가 존재하면 OpenSearch에 인덱싱한다")
    void givenIndexActionAndContentExists_whenConsume_thenIndexContent() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        Content content = Content.builder()
                .title("테스트 콘텐츠")
                .build();
        String message = objectMapper.writeValueAsString(
                new ContentIndexMessage(contentId, ContentIndexMessage.ActionType.INDEX));

        given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

        // when
        contentIndexConsumer.consume(message);

        // then
        then(contentRepository).should().findById(contentId);
        then(contentIndexService).should().index(content);
    }

    @Test
    @DisplayName("INDEX 액션 - 콘텐츠가 존재하지 않으면 인덱싱하지 않는다")
    void givenIndexActionAndContentNotFound_whenConsume_thenSkipIndexing() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        String message = objectMapper.writeValueAsString(
                new ContentIndexMessage(contentId, ContentIndexMessage.ActionType.INDEX));

        given(contentRepository.findById(contentId)).willReturn(Optional.empty());

        // when
        contentIndexConsumer.consume(message);

        // then
        then(contentRepository).should().findById(contentId);
        then(contentIndexService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("DELETE 액션 - OpenSearch에서 인덱스를 삭제한다")
    void givenDeleteAction_whenConsume_thenDeleteIndex() throws Exception {
        // given
        UUID contentId = UUID.randomUUID();
        String message = objectMapper.writeValueAsString(
                new ContentIndexMessage(contentId, ContentIndexMessage.ActionType.DELETE));

        // when
        contentIndexConsumer.consume(message);

        // then
        then(contentIndexService).should().delete(contentId);
        then(contentRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("잘못된 메시지 형식이면 RuntimeException을 던진다")
    void givenInvalidMessage_whenConsume_thenThrowRuntimeException() {
        // given
        String invalidMessage = "invalid json";

        // when & then
        assertThatThrownBy(() -> contentIndexConsumer.consume(invalidMessage))
                .isInstanceOf(RuntimeException.class);
    }
}