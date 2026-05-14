package com.mopl.mopl.domain.notification.service.listener;

import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.config.AsyncConfig;
import com.mopl.mopl.global.event.DirectMessageCreatedEvent;
import com.mopl.mopl.global.event.FollowEvent;
import com.mopl.mopl.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final SseService sseService;
  private final NotificationMapper notificationMapper;
  private final RoomPresenceManager roomPresenceManager;

  /**
   * 팔로우 이벤트 리스너
   * @TransactionalEventListener(AFTER_COMMIT) : 메인 트랜잭션(팔로우 DB 저장)이 완전히 성공한 후에만 알림을 보냄
   * @Async : 알림 발송이 메인 쓰레드의 응답을 지연시키지 않도록 별도 쓰레드에서 비동기 처리
   * @param event 팔로우 이벤트 객체
   */
  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFollowEvent(FollowEvent event) {

    log.debug("팔로우 이벤트 수신 - followerId: {}, followeeId: {}", event.followerId(), event.followeeId());

    // 알림 수신자(Followee) 엔티티 조회
    User receiver = userRepository.getReferenceById(event.followeeId());

    // 알림 메시지 생성
    String title = "새로운 팔로워";
    String content = event.followerName() + "님이 회원님을 팔로우하기 시작했습니다.";

    // Notification db 저장
    Notification notification = Notification.builder()
        .user(receiver)
        .title(title)
        .content(content)
        .level(NotificationLevel.INFO)
        .build();

    Notification saved = notificationRepository.save(notification);

    // SSE 푸시 전송
    NotificationDto dto = notificationMapper.toDto(saved);
    sseService.sendNotification(receiver.getId(), dto);
  }

  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDirectMessageEvent(DirectMessageCreatedEvent event) {

    log.debug("DM 수신 알림 DB 저장 이벤트 수신 - receiverId: {}", event.receiverId());

    User receiver = userRepository.getReferenceById(event.receiverId());

    // 보낸 사람 이름 추출
    String senderName = event.messageDto().sender().name() != null ? event.messageDto().sender().name() : "누군가";
    // 메시지 내용 (최대 50자 제한)
    String messagePreview = truncateMessage(event.messageDto().content(), 50);

    String title = "새로운 메시지";
    // 알림 리스트에서 보여줄 메시지 미리보기
    String content = senderName + "님: " + messagePreview;

    Notification notification = Notification.builder()
        .user(receiver)
        .title(title)
        .content(content)
        .level(NotificationLevel.INFO)
        .build();

    Notification saved = notificationRepository.save(notification);
    NotificationDto dto = notificationMapper.toDto(saved);

    boolean isInRoom = roomPresenceManager.isUserInRoom(event.receiverId(), event.conversationId());
    if (!isInRoom) {
      sseService.sendNotification(receiver.getId(), dto);
    }
  }

  private String truncateMessage(String message, int maxLength) {
    if (message == null || message.length() <= maxLength) {
      return message;
    }

    return message.substring(0, maxLength) + "...";
  }
}