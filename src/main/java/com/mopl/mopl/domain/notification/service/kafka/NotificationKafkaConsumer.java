package com.mopl.mopl.domain.notification.service.kafka;

import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.domain.follow.entity.Follow;
import com.mopl.mopl.domain.follow.repository.FollowRepository;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.entity.NotificationLevel;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.playlist.entity.PlaylistSubscription;
import com.mopl.mopl.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.*;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import com.mopl.mopl.global.sse.service.SseService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final SseService sseService;
  private final NotificationMapper notificationMapper;
  private final FollowRepository followRepository;
  private final RoomPresenceManager roomPresenceManager;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final BadWordNotificationProcessor badWordNotificationProcessor;

  /**
   * 팔로우 이벤트 수신
   * @param event 팔로우 이벤트 객체
   */
  @KafkaListener(topics = "notification-follow-topic", groupId = "mopl-group")
  public void consumeFollowEvent(FollowEvent event) {
    log.debug("[Kafka Consumer] FollowEvent 수신 완료 및 알림 생성 시작");

    User receiver = userRepository.getReferenceById(event.followeeId());

    String title = "새로운 팔로워";
    String content = event.followerName() + "님이 회원님을 팔로우하기 시작했습니다.";

    Notification notification = Notification.builder()
        .user(receiver)
        .title(title)
        .content(content)
        .level(NotificationLevel.INFO)
        .build();

    Notification saved = notificationRepository.save(notification);
    NotificationDto dto = notificationMapper.toDto(saved);

    sseService.sendNotification(receiver.getId(), dto);
  }

  /**
   * 리뷰 이벤트 수신
   * @param event 리뷰 이벤트 객체
   */
  @KafkaListener(topics = "notification-review-topic", groupId = "mopl-group")
  public void consumeReviewEvent(ReviewCreatedEvent event) {
    log.debug("[Kafka Consumer] ReviewEvent 수신 완료 및 알림 생성 시작");

    // 작성자를 팔로우하는 모든 팔로워 목록 조회
    List<Follow> follows = followRepository.findAllByFolloweeIdWithFollower(event.writerId());

    // 팔로워가 아무도 없으면 리턴
    if (follows.isEmpty()) {
      log.debug("팔로워가 없어 알림을 발송하지 않습니다 - writerId: {}", event.writerId());
      return;
    }

    String title = "새로운 활동";
    String content = event.writerName() + "님이 새로운 리뷰를 작성했습니다.";

    // 팔로워 수 만큼 Notification 엔티티 생성
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

    savedNotifications.forEach(saved -> {
      NotificationDto dto = notificationMapper.toDto(saved);
      sseService.sendNotification(saved.getUser().getId(), dto);
    });

    log.info("리뷰 작성 알림 브로드캐스팅 완료 - 발송 건수: {}", savedNotifications.size());
  }

  /**
   * DM 이벤트 수신
   * @param event DM 이벤트 객체
   */
  @KafkaListener(topics = "notification-dm-topic", groupId = "mopl-group")
  public void consumeDirectMessageEvent(DirectMessageCreatedEvent event) {

    boolean isInRoom = roomPresenceManager.isUserInRoom(event.receiverId(), event.conversationId());
    if (isInRoom) {
      // 방에 있으면 DB 저장, SSE 발송 X : Early Return
      log.debug("유저가 이미 채팅방 안에 있으므로 알림 저장/발송을 생략합니다.");
      return;
    }
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

    sseService.sendNotification(receiver.getId(), dto);
  }

  @KafkaListener(topics = "notification-role-topic", groupId = "mopl-group")
  public void consumeUserUpdateRoleEvent(UserUpdateRoleEvent event) {

    log.debug("[Kafka Consumer] UserUpdateRoleEvent 수신 및 알림 생성");

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

  @KafkaListener(topics = "notification-playlist-subscribe-topic", groupId = "mopl-group")
  public void consumePlaylistSubscribedEvent(PlaylistSubscribedEvent event) {

    log.debug("[kafka Consumer] PlaylistSubscribedEvent 수신 및 알림 생성");

    // 플레이리스트 주인이 수신자
    User receiver = userRepository.getReferenceById(event.playlistOwnerId());

    Notification notification = Notification.builder()
        .user(receiver)
        .title("새로운 구독자")
        .content(event.subscriberName() + "님이 회원님의 [" + event.playlistTitle() + "] 플레이리스트를 구독했습니다.")
        .level(NotificationLevel.INFO)
        .build();

    Notification saved = notificationRepository.save(notification);
    NotificationDto dto = notificationMapper.toDto(saved);
    sseService.sendNotification(receiver.getId(), dto);
  }

  @KafkaListener(topics = "notification-playlist-content-topic", groupId = "mopl-group")
  public void consumePlaylistContentAddedEvent(PlaylistContentAddedEvent event) {

    log.debug("[Kafka Consumer] PlaylistContentAddedEvent 수신 및 알림 생성");

    // 구독자 가져오기
    List<PlaylistSubscription> subscriptions = playlistSubscriptionRepository.findAllByPlaylistIdWithUser(event.playlistId());
    if (subscriptions.isEmpty()) {
      log.debug("구독자가 없어 알림을 발송하지 않습니다 - playlistId: {}", event.playlistId());
      return;
    }

    List<Notification> notifications = subscriptions.stream()
        .map(sub -> Notification.builder()
            .user(sub.getUser())
            .title("플레이리스트 업데이트")
            .content("구독 중인 [" + event.playlistTitle() + "] 플레이리스트에 새로운 콘텐츠가 추가되었습니다.")
            .level(NotificationLevel.INFO)
            .build())
        .toList();

    // 벌크 인서트
    List<Notification> savedNotifications = notificationRepository.saveAll(notifications);

    // 알림 전송
    savedNotifications.forEach(saved -> {
      NotificationDto dto = notificationMapper.toDto(saved);
      sseService.sendNotification(saved.getUser().getId(), dto);
    });

    log.info("플레이리스트 업데이트 알림 브로드캐스팅 완료 - 발송 건수: {}", savedNotifications.size());
  }

  private String truncateMessage(String message, int maxLength) {
    if (message == null || message.length() <= maxLength) {
      return message;
    }

    return message.substring(0, maxLength) + "...";
  }

  @KafkaListener(topics = "notification-badWord-topic", groupId = "mopl-group")
  public void onBadWordDetected(BadWordDetectedEvent event) {
    log.info("[Kafka Consumer] BadWordDetectedEvent 수신 - userId: {}, content: {}",
            event.userId(), event.content());
    badWordNotificationProcessor.processBadWordDetected(event);
  }
}
