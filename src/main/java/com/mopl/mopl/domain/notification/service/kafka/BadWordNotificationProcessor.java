package com.mopl.mopl.domain.notification.service.kafka;

import com.mopl.mopl.domain.jwt.registry.JwtRegistry;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.BadWordDetectedEvent;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadWordNotificationProcessor {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SseService sseService;
    private final JwtRegistry jwtRegistry;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BAN_STATUS_VALUE = "banned";
    private static final long MAX_BAN_DURATION_SECONDS = 86400 * 30; // 30일

    public static final String BAN_KEY_PREFIX = "users:banned:ttl:";


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBadWordDetected(BadWordDetectedEvent event) {
        UUID userId = event.userId();
        log.info("[BadWordDetectedEventListener] 욕설 감지 이벤트 수신 userId: {}, content: {}",
                userId,
                event.content()
        );

        User receiver = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        receiver.increaseWarningCount();

        if (receiver.isLocked()) {
            jwtRegistry.invalidateJwtInformationByUserId(userId);
        }

        Notification notification = Notification.builder()
                .user(receiver)
                .title(resolveTitle(receiver))
                .content(resolveContent(receiver))
                .level(NotificationLevel.INFO)
                .build();

        Notification saved = notificationRepository.saveAndFlush(notification);
        NotificationDto dto = notificationMapper.toDto(saved);

        try {
            log.info("[Redis] 실시간 SSE 알림을 알림 발행 - targetUserid: {}", userId);
            sseService.sendNotification(userId, dto);
        } catch (Exception e) {
            log.error("[SSE] 실시간 알림 발송 중 예외 발생 - userId: {}", userId);
        }

        // 유저 벤 해제
        // 1. 유저가 3번의 경고로 벤을 당했을 경우,
        // 2. 9번의 경고로 락이 걸리지 않았을 경우
        if (receiver.isBanned() && !receiver.isLocked()) {
            LocalDateTime banExpiresAt = receiver.getBanExpiresAt();
            if (banExpiresAt != null) {
                long durationSeconds = Duration.between(
                        LocalDateTime.now(),
                        banExpiresAt
                ).getSeconds();

                if (durationSeconds > 0 && durationSeconds <= MAX_BAN_DURATION_SECONDS) {
                    String redisKey = BAN_KEY_PREFIX + userId.toString();
                    try {
                        redisTemplate.opsForValue().set(
                            redisKey,
                            BAN_STATUS_VALUE,
                            durationSeconds,
                            TimeUnit.SECONDS
                        );
                        log.info("[Redis TTL] 실시간 유저 벤 해제 TTL 설정 완료. userId: {}, {}초 뒤 만료", userId, durationSeconds);
                    } catch (Exception e) {
                        log.error("[Redis TTL] TTL 설정 실패. 스케줄러가 백업 처리 예정. userId: {}", userId, e);
                    }
                } else if (durationSeconds > MAX_BAN_DURATION_SECONDS) {
                    log.warn("[Redis TTL] 정지 기간이 최대 허용치를 초과하여 TTL 미설정. userId: {}, duration: {}초", userId, durationSeconds);
                }
            }
        }
        log.info("[BadWordNotificationProcessor] 처리 완료 - userId: {}, warningCount: {}", userId, receiver.getWarningCount());
    }

    private String resolveTitle(User receiver) {
        if (!receiver.isBanned()) {
            return "🚨 욕설 사용으로 인한 경고 [누적 경고: " + receiver.getWarningCount() + "]";
        }

        int banCount = receiver.getWarningCount() / 3;
        return switch (banCount) {
            case 1 -> "🚫 [1차 정지] 실시간 채팅 5분 이용 금지";
            case 2 -> "🚫 [2차 정지] 실시간 채팅 1시간 이용 금지";
            default -> "🔒 [영구 정지] 모든 활동이 정지됩니다.";
        };
    }

    private String resolveContent(User receiver) {
        String base = "부적절한 표현이 감지되어 마스킹 처리되었습니다. \n";
        if (!receiver.isBanned()) {
            return base + "바른 언어 사용을 부탁드립니다. 🙏";
        }

        int banCount = receiver.getWarningCount() / 3;
        String detail = switch (banCount) {
            case 1 -> "5분간 실시간 채팅 이용이 제한됩니다.";
            case 2 -> "1시간 실시간 채팅 이용이 제한됩니다.";
            default -> "모든 서비스 이용이 영구 제한됩니다. 관리자에게 문의하세요.";
        };

        return base + detail;
    }
}