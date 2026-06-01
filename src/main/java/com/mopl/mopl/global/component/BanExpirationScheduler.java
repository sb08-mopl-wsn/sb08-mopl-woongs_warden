package com.mopl.mopl.global.component;

import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BanExpirationScheduler {

    private final UserRepository userRepository;
    private final UserUnbanProcessor userUnbanProcessor;
    private static final Logger log = LoggerFactory.getLogger(BanExpirationScheduler.class);

    // TODO: 추후 Redis TTL 도입으로 정확도를 개선하고 시스템 부하를 줄일 예정
    @Scheduled(fixedDelay = 30000)
    public void unbanExpiredUsers() {

        List<User> expiredUsers = userRepository
                .findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(
                        LocalDateTime.now()
                );

        if (expiredUsers.isEmpty()) {
            return;
        }

        log.info("[BanExpirationScheduler] 정지 해제 유저: {}명", expiredUsers.size());

        expiredUsers.forEach(user -> {
            try {
                userUnbanProcessor.processUnban(user);
            } catch (Exception e) {
                log.error("[BanExpirationScheduler] 정지 해제 실패 userId: {}", user.getId(), e);
            }
        });
    }
}
