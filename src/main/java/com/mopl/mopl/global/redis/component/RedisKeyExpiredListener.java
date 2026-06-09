package com.mopl.mopl.global.redis.component;

import com.mopl.mopl.domain.notification.service.kafka.BadWordNotificationProcessor;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.component.UserUnbanProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.util.UUID;

@Slf4j
public class RedisKeyExpiredListener implements MessageListener {

    private final UserRepository userRepository;
    private final UserUnbanProcessor userUnbanProcessor;

    public RedisKeyExpiredListener(
            UserRepository userRepository,
            UserUnbanProcessor userUnbanProcessor
    ) {
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
                userRepository.findById(userId).ifPresentOrElse(
                        user -> {
                            try {
                                userUnbanProcessor.processUnban(user);
                            } catch (Exception e) {
                                log.error("[Redis Listener] 정지 해제 처리 중 오류 발생. userId: {}", userIdStr, e);
                            }
                        },
                        () -> log.warn("[Redis Listener] 정지 해제 대상 유저를 찾을 수 없음. userId: {}", userIdStr)
                );
            } catch (IllegalArgumentException e) {
                log.error("[Redis Listener] 잘못된 UUID 형식의 키 감지. userIdStr: {}", userIdStr, e);
            }
        }
    }
}
