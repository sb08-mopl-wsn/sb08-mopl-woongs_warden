package com.mopl.mopl.global.redis.component;

import com.mopl.mopl.domain.watchingSession.dto.response.ContentChatDto;
import com.mopl.mopl.domain.watchingSession.dto.response.WatchingSessionChange;
import com.mopl.mopl.global.event.LiveChatEvent;
import com.mopl.mopl.global.event.WatchingSessionEvent;
import com.mopl.mopl.global.redis.dto.WebsocketPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WatchingSessionRedisPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ChannelTopic watchTopic;

    @Mock
    private ChannelTopic chatTopic;

    @Mock
    private WatchingSessionChange watchingSessionChange;

    @Mock
    private ContentChatDto contentChatDto;

    private WatchingSessionRedisPublisher publisher;

    private static final String WATCH_TOPIC = "watch-topic";
    private static final String CHAT_TOPIC = "chat-topic";

    @BeforeEach
    void setUp() {
        publisher = new WatchingSessionRedisPublisher(redisTemplate, watchTopic, chatTopic);
    }

    @Nested
    @DisplayName("시청 세션 이벤트 발행")
    class WatchingSessionEventTests {

        @Test
        @DisplayName("watchTopic 으로 발행한다")
        void shouldPublishToWatchTopic() {
            // given
            when(watchTopic.getTopic()).thenReturn(WATCH_TOPIC);
            UUID contentId = UUID.randomUUID();
            WatchingSessionEvent event = new WatchingSessionEvent(watchingSessionChange, contentId);

            // when
            publisher.handleWatchingSessionEvent(event);

            // then
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(redisTemplate).convertAndSend(
                    ArgumentMatchers.eq(WATCH_TOPIC),
                    payloadCaptor.capture()
            );

            WebsocketPayload<?> captured = (WebsocketPayload<?>) payloadCaptor.getValue();
            assertThat(captured.contentId()).isEqualTo(contentId);
            assertThat(captured.targetType()).isEqualTo("WATCH");
            assertThat(captured.data()).isEqualTo(watchingSessionChange);
        }

        @Test
        @DisplayName("chatTopic 으로 발행하지 않는다")
        void shouldNotPublishToChatTopic() {
            // given
            when(watchTopic.getTopic()).thenReturn(WATCH_TOPIC);
            UUID contentId = UUID.randomUUID();
            WatchingSessionEvent event = new WatchingSessionEvent(watchingSessionChange, contentId);

            // when
            publisher.handleWatchingSessionEvent(event);

            // then
            verify(redisTemplate).convertAndSend(
                    org.mockito.ArgumentMatchers.eq(WATCH_TOPIC),
                    org.mockito.ArgumentMatchers.any(Object.class)
            );
            verify(redisTemplate, org.mockito.Mockito.never()).convertAndSend(
                    org.mockito.ArgumentMatchers.eq(CHAT_TOPIC),
                    org.mockito.ArgumentMatchers.any(Object.class)
            );
        }
    }

    @Nested
    @DisplayName("라이브 채팅 이벤트 발행")
    class LiveChatEventTests {

        @Test
        @DisplayName("chatTopic 으로 발행한다")
        void shouldPublishToChatTopic() {
            // given
            when(chatTopic.getTopic()).thenReturn(CHAT_TOPIC);
            UUID contentId = UUID.randomUUID();
            LiveChatEvent event = new LiveChatEvent(contentId, contentChatDto);

            // when
            publisher.handleLiveChatEvent(event);

            // then
            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(redisTemplate).convertAndSend(
                    ArgumentMatchers.eq(CHAT_TOPIC),
                    payloadCaptor.capture()
            );

            WebsocketPayload<?> captured = (WebsocketPayload<?>) payloadCaptor.getValue();
            assertThat(captured.contentId()).isEqualTo(contentId);
            assertThat(captured.targetType()).isEqualTo("CHAT");
            assertThat(captured.data()).isEqualTo(contentChatDto);
        }

        @Test
        @DisplayName("watchTopic 으로 발행하지 않는다")
        void shouldNotPublishToWatchTopic() {
            // given
            when(chatTopic.getTopic()).thenReturn(CHAT_TOPIC);
            UUID contentId = UUID.randomUUID();
            LiveChatEvent event = new LiveChatEvent(contentId, contentChatDto);

            // when
            publisher.handleLiveChatEvent(event);

            // then
            verify(redisTemplate).convertAndSend(
                    ArgumentMatchers.eq(CHAT_TOPIC),
                    ArgumentMatchers.any(Object.class)
            );
            verify(redisTemplate, org.mockito.Mockito.never()).convertAndSend(
                    ArgumentMatchers.eq(WATCH_TOPIC),
                    ArgumentMatchers.any(Object.class)
            );
        }
    }
}
