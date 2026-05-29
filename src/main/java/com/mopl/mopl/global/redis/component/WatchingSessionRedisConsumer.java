package com.mopl.mopl.global.redis.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.global.redis.dto.WebsocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
@Component
public class WatchingSessionRedisConsumer implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private static final String DESTINATION_PREFIX = "/sub/contents/";
    private static final String SESSION_DESTINATION_SUFFIX = "/watch";
    private static final String LIVE_CHAT_DESTINATION_SUFFIX = "/chat";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            log.info("[Redis Consumer] 메시지 수신 채널: {}", channel);

            WebsocketPayload<?> payload = objectMapper.readValue(
                    message.getBody(),
                    new TypeReference<WebsocketPayload<Object>>() {
                    }
            );

            String destination = resolveDestination(channel, payload.contentId());
            if (destination == null) return;

            log.info("[Redis Consumer] 인스턴스 라우팅 destination: {}", destination);
            messagingTemplate.convertAndSend(destination, payload.data());

        } catch (Exception e) {
            log.error("Redis Pub/Sub 메시지 처리 중 오류 발생", e);
        }
    }

    private String resolveDestination(String channel, Object contentId) {
        String base = DESTINATION_PREFIX + contentId;
        if (channel.contains("watch")) return base + SESSION_DESTINATION_SUFFIX;
        if (channel.contains("chat")) return base + LIVE_CHAT_DESTINATION_SUFFIX;

        log.warn("[Redis Consumer] 알 수 없는 채널: {}", channel);
        return null;
    }
}