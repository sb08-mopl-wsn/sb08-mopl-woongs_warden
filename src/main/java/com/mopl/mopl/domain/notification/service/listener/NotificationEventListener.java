package com.mopl.mopl.domain.notification.service.listener;

import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.domain.follow.entity.Follow;
import com.mopl.mopl.domain.follow.repository.FollowRepository;
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
import com.mopl.mopl.global.event.ReviewCreatedEvent;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import com.mopl.mopl.global.sse.service.SseService;
import java.util.List;
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
  private final FollowRepository followRepository;
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

  /**
   * DM 이벤트 리스너
   * @param event DM 이벤트 객체
   */
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

  /**
   * 유저 권한 변경 알림 수신 리스너
   * @param event 유저 권한 변경 이벤트 객체
   */
  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserUpdateRoleEvent(UserUpdateRoleEvent event) {

    log.debug("권한 변경 알림 이벤트 수신 - userId: {}, newRole: {}", event.userId(), event.role());

    User receiver = userRepository.getReferenceById(event.userId());

    Notification notification = Notification.builder()
        .user(receiver)
        .title("권한 변경 안내")
        .content(event.name() + "님의 계정 권한이 [" + event.role().name() + "] (으)로 변경되었습니다.")
        .level(NotificationLevel.INFO)
        .build();

    Notification saved = notificationRepository.save(notification);
    NotificationDto dto = notificationMapper.toDto(saved);
    sseService.sendNotification(receiver.getId(), dto);
  }

  @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleReviewCreatedEvent(ReviewCreatedEvent event) {

    log.debug("리뷰 작성 알림 이벤트 수신 - writerId: {}", event.writerId());

    // 작성자를 팔로우하는 모든 팔로워 목록 조회
    List<Follow> follows = followRepository.findAllByFolloweeId(event.writerId());

    // 팔로워가 아무도 없으면 리턴
    if (follows.isEmpty()) return;

    String title = "새로운 활동";
    String content = event.writerName() + "님이 새로운 리뷰를 작성했습니다.";

    // 팔로워 수 만큼 Notification 엔티티 생성 (JPA 지연로딩)
    List<Notification> notifications = follows.stream()
        .map(follow -> Notification.builder()
            .user(follow.getFollower())
            .title(title)
            .content(content)
            .level(NotificationLevel.INFO)
            .build())
        .toList();

    // saveAll로 저장
    List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

    // 저장된 알림들 DTO로 변환해서 SSE 알림 발송
    savedNotifications.forEach(saved -> {
      NotificationDto dto = notificationMapper.toDto(saved);
      sseService.sendNotification(saved.getUser().getId(), dto);
    });

    log.info("리뷰 작성 알림 브로드캐스팅 완료 - 발송 건수: {}", savedNotifications.size());
  }

  private String truncateMessage(String message, int maxLength) {
    if (message == null || message.length() <= maxLength) {
      return message;
    }

    return message.substring(0, maxLength) + "...";
  }
}