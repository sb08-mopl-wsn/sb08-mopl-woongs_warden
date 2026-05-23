package com.mopl.mopl.global.component;

import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BanExpirationScheduler {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SseService sseService;

    // TODO: 추후 Redis TTL 도입으로 정확도를 개선하고 시스템 부하를 줄일 예정
    @Scheduled(fixedDelay = 60000)
    @Transactional
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
                user.unBan();

                Notification notification = Notification.builder()
                        .user(user)
                        .title("✅ 채팅 이용 제한이 해제되었습니다.")
                        .content("다시 채팅에 참여하실 수 있습니다. 바르고 고운말 부탁드립니다.")
                        .level(NotificationLevel.INFO)
                        .build();
                Notification savedNotification = notificationRepository.save(notification);
                sseService.sendNotification(user.getId(), notificationMapper.toDto(savedNotification));

                log.info("[BanExpirationScheduler] 정지 해제 완료 userId: {}", user.getId());
            } catch (Exception e) {
                log.error("[BanExpirationScheduler] 정지 해제 실패 userId: {}", user.getId(), e);
            }
        });
    }
}
