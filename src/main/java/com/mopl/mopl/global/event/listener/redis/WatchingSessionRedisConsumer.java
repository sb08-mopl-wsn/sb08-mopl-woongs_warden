package com.mopl.mopl.global.event.listener.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.mopl.global.event.dto.WebsocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class WatchingSessionRedisConsumer implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final String DESTINATION_PREFIX = "/sub/contents/";
    private static final String SESSION_DESTINATION_SUFFIX = "/watch";
    private static final String LIVE_CHAT_DESTINATION_SUFFIX = "/chat";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            log.info("[Redis Consumer] 메시지 수신 채널: {}", channel);

            // byte[] → WebsocketPayload 직접 역직렬화
            WebsocketPayload<?> payload = redisObjectMapper.readValue(
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