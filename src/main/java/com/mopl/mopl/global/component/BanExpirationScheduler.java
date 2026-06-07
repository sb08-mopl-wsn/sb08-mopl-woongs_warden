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

    /**
     * [새벽 백업용 스케줄러]
     * 매일 새벽 4시 0분 0초에 딱 한번만 실행한다.
     * 배포나 장애로 인해 Redis 이벤트를 놓쳐 풀리지 못한 유저의 벤을 풀어준다.
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void unbanExpiredUsers() {

        log.info("[BanBackupScheduler] 새벽 벤 미해제 유저 백업 스캔 시작...");

        List<User> expiredUsers = userRepository
                .findAllByIsBannedTrueAndBanExpiresAtBeforeAndIsLockedFalse(
                        LocalDateTime.now()
                );

        if (expiredUsers.isEmpty()) {
            log.info("[BanBackupScheduler] 벤 미해제 유저가 없습니다.");
            return;
        }

        log.warn("[BanExpirationScheduler] 인프라 장애로 누수되었던 유저 {}명을 발견하여 강제 해제합니다.", expiredUsers.size());

        expiredUsers.forEach(user -> {
            try {
                userUnbanProcessor.processUnban(user);
            } catch (Exception e) {
                log.error("[BanExpirationScheduler] 정지 해제 실패 userId: {}", user.getId(), e);
            }
        });
    }
}
