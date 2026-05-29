package com.mopl.mopl.global.redis.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mopl.mopl.global.redis.dto.WebsocketPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;


@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionRedisConsumer 테스트")
public class WatchingSessionRedisConsumerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WatchingSessionRedisConsumer consumer;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        consumer = new WatchingSessionRedisConsumer(messagingTemplate, objectMapper);
    }

    private Message createMessage(String channel, byte[] body) {
        return new Message() {
            @Override
            public byte[] getBody() {
                return body;
            }

            @Override
            public byte[] getChannel() {
                return channel.getBytes();
            }
        };
    }

    private <T> byte[] toJson(WebsocketPayload<T> payload) throws Exception {
        return objectMapper.writeValueAsBytes(payload);
    }

    @Nested
    @DisplayName("watch 채널 수신")
    class WatchChannelTests {

        @Test
        @DisplayName("watch 채널 메시지를 수신하면 /sub/contents/{contentId}/watch로 전송한다")
        void shouldSendToWatchDestination() throws Exception {
            // given
            UUID contentId = UUID.randomUUID();
            String sessionData = "session-data";
            WebsocketPayload<String> payload = new WebsocketPayload<>(
                    contentId,
                    "WATCH",
                    "session-data"
            );

            Message message = createMessage("room:watch:" + contentId, toJson(payload));

            // when
            consumer.onMessage(message, null);

            // then
            ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/contents/" + contentId + "/watch"),
                    dataCaptor.capture()
            );
            assertThat(dataCaptor.getValue()).isEqualTo("session-data");
        }

        @Test
        @DisplayName("watch 채널에서 data가 Map 타입이어도 정상 전송한다.")
        void shouldSendWatchWithMapData() throws Exception {
            //given
            UUID contentId = UUID.randomUUID();
            var data = Map.of("userId", "user-1", "status", "joined");
            WebsocketPayload<Object> payload = new WebsocketPayload<>(
                    contentId,
                    "WATCH",
                    data
            );

            Message message = createMessage("watch:session", toJson(payload));

            // when
            consumer.onMessage(message, null);

            // then
            verify(messagingTemplate).convertAndSend(
                    eq("/sub/contents/" + contentId + "/watch"),
                    any(Object.class)
            );
        }

        @Nested
        @DisplayName("chat 채널 수신")
        class ChatChannelTests {

            @Test
            @DisplayName("chat 채널 수신 시 /sub/contents/{contentId}/chat 으로 전송한다.")
            void shouldSendToChatDestination() throws Exception {
                // given
                UUID contentId = UUID.randomUUID();
                WebsocketPayload<String> payload = new WebsocketPayload<>(
                        contentId,
                        "CHAT",
                        "안녕하세요?"
                );

                Message message = createMessage("room:chat:" + contentId, toJson(payload));

                // when
                consumer.onMessage(message, null);

                // then
                ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);
                verify(messagingTemplate).convertAndSend(
                        eq("/sub/contents/" + contentId + "/chat"),
                        dataCaptor.capture()
                );
                assertThat(dataCaptor.getValue()).isEqualTo("안녕하세요?");
            }
        }

        @Nested
        @DisplayName("알 수 없는 채널 수신")
        class UnknownChannelTests {

            @Test
            @DisplayName("watch / chat이 아닌 채널이면 메시지를 전송하지 않는다.")
            void shouldNotSendForUnknownChannel() throws Exception {
                // given
                UUID contentId = UUID.randomUUID();
                WebsocketPayload<String> payload = new WebsocketPayload<>(
                        contentId,
                        "WATCH",
                        "data"
                );
                Message message = createMessage("unknown:channel", toJson(payload));

                // when
                consumer.onMessage(message, null);

                // then
                verifyNoInteractions(messagingTemplate);
            }
        }
    }
}
