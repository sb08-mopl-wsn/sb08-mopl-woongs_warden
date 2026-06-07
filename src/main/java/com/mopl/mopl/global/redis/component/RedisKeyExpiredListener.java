package com.mopl.mopl.global.redis.component;

import com.mopl.mopl.domain.notification.service.kafka.BadWordNotificationProcessor;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.component.UserUnbanProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.UUID;

@Slf4j
public class RedisKeyExpiredListener extends KeyExpirationEventMessageListener {

    private final UserRepository userRepository;
    private final UserUnbanProcessor userUnbanProcessor;

    public RedisKeyExpiredListener(
            RedisMessageListenerContainer listenerContainer,
            UserRepository userRepository,
            UserUnbanProcessor userUnbanProcessor
    ) {
        super(listenerContainer);
        this.userRepository = userRepository;
        this.userUnbanProcessor = userUnbanProcessor;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith(BadWordNotificationProcessor.BAN_KEY_PREFIX)) {
            String userIdStr = expiredKey.replace(BadWordNotificationProcessor.BAN_KEY_PREFIX, "");

            log.info("[Redis Listener] 실시간 벤 유저 프로세스 실행. userId: {}", userIdStr);

            try {
                UUID userId = UUID.fromString(userIdStr);
                userRepository.findById(userId).ifPresent(userUnbanProcessor::processUnban);
            } catch (Exception e) {
                log.error("[Redis Listener] 실시간 정지 해제 실패. userId: {}", userIdStr, e);
            }
        }
    }
}
