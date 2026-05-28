package com.mopl.mopl.global.component;

import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 정지 해제 + 알림 생성 + SSE 전송을 하나의 트랜잭션으로 처리하는 컴포넌트
@Component
@RequiredArgsConstructor
public class UserUnbanProcessor {

    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final SseService sseService;
    private final NotificationMapper notificationMapper;
    private static final Logger log = LoggerFactory.getLogger(UserUnbanProcessor.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processUnban(User user) {
        user.unBan();
        // 더티 체킹이 일어나도 영속성 컨텍스트 관리를 위해 명시적 호출
        userRepository.save(user);

        Notification notification = Notification.builder()
                .user(user)
                .title("✅ 채팅 이용 제한이 해제되었습니다.")
                .content("다시 채팅에 참여하실 수 있습니다. 바르고 고운말 부탁드립니다.")
                .level(NotificationLevel.INFO)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        sseService.sendNotification(user.getId(), notificationMapper.toDto(savedNotification));

        log.info("[BanExpirationScheduler] 정지 해제 완료 userId: {}", user.getId());
    }
}
