package com.mopl.mopl.global.event.listener;

import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.config.AsyncConfig;
import com.mopl.mopl.global.event.BadWordDetectedEvent;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class BadWordDetectedEventListener {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SseService sseService;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBadWordDetected(BadWordDetectedEvent event) {

        UUID userId = event.userId();
        log.info("[BadWordDetectedEventListener] 욕설 감지 이벤트 수신 userId: {}, content: {}", userId, event.content());

        try {
            User receiver = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            receiver.increaseWarningCount();

            Notification notification = Notification.builder()
                    .user(receiver)
                    .title(resolveTitle(receiver))
                    .content(resolveContent(receiver))
                    .level(NotificationLevel.INFO)
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            sseService.sendNotification(userId, notificationMapper.toDto(savedNotification));

        } catch (Exception e) {
            log.error("[BadWordDetectedEventListener] 경고 알림 중 에러 발생 userId: {}", userId, e);
        }
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
