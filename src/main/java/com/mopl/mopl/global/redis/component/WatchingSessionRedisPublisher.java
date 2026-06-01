package com.mopl.mopl.global.redis.component;

import com.mopl.mopl.global.event.LiveChatEvent;
import com.mopl.mopl.global.event.WatchingSessionEvent;
import com.mopl.mopl.global.redis.dto.WebsocketPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class WatchingSessionRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic watchTopic;
    private final ChannelTopic chatTopic;

    @Async("watchingSessionExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWatchingSessionEvent(WatchingSessionEvent event) {
        log.info("[Redis Publisher] 시청 세션 변경 이벤트 발행. contentId: {}, Type: {}",
                event.contentId(), event.change().type());

        WebsocketPayload<?> payload = new WebsocketPayload<>(
                event.contentId(),
                "WATCH",
                event.change()
        );

        redisTemplate.convertAndSend(watchTopic.getTopic(), payload);
    }

    @Async("watchingSessionExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLiveChatEvent(LiveChatEvent event) {
        log.info("[Redis Publisher] 라이브 채팅 메시지 발생. contentId: {}", event.contentId());

        WebsocketPayload<?> payload = new WebsocketPayload<>(
                event.contentId(),
                "CHAT",
                event.chatDto()
        );

        redisTemplate.convertAndSend(chatTopic.getTopic(), payload);
    }
}
