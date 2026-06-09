package com.mopl.mopl.domain.notification.service.kafka;

import com.mopl.mopl.global.event.*;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaProducer {

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * 팔로우 이벤트 발행
   * @param event 팔로우 이벤트 객체
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void produceFollowEvent(FollowEvent event) {

    log.info("[Kafka Producer] FollowEvent 발행 - followerId: {}", event.followerId());

    // 토픽에 수신자 ID를 파티션 키로 이벤트 발행
    kafkaTemplate.send("notification-follow-topic", event.followeeId().toString(), event);
  }

  /**
   * 리뷰 이벤트 발행
   * @param event 리뷰 이벤트 객체
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void produceReviewEvent(ReviewCreatedEvent event) {

    log.info("[Kafka Producer] ReviewEvent 발행 - writerId: {}", event.writerId());

    kafkaTemplate.send("notification-review-topic", event.writerId().toString(), event);
  }

  /**
   * DM 이벤트 발행
   * @param event DM 이벤트 객체
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void produceDirectMessageEvent(DirectMessageCreatedEvent event) {

    log.info("[Kafka Producer] DirectMessageEvent 발행 - receiverId: {}", event.receiverId());

    kafkaTemplate.send("notification-dm-topic", event.receiverId().toString(), event);
  }

  /**
   * 유저 권한 업데이트 이벤트 발행
   * @param event 유저 권한 업데이트 이벤트 객체
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void produceUserUpdateRoleEvent(UserUpdateRoleEvent event) {

    log.info("[Kafka Producer] UserUpdateRoleEvent 발행 - userId: {}", event.userId());

    kafkaTemplate.send("notification-role-topic", event.userId().toString(), event);
  }

  /**
   * 플레이리스트 구독 이벤트 발행
   * @param event 플레이리스트 구독 이벤트 객체
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void producePlaylistSubscribedEvent(PlaylistSubscribedEvent event) {

    log.info("[Kafka Producer] PlaylistSubscribedEvent 발행 - playlistOwnerId: {}", event.playlistOwnerId());

    kafkaTemplate.send("notification-playlist-subscribe-topic", event.playlistOwnerId().toString(), event);
  }

  /**
   * 플레이리스트 콘텐츠 추가 이벤트 발행
   * @param event 플레이리스트 콘텐츠 추가 이벤트 객체
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void producePlaylistContentAddedEvent(PlaylistContentAddedEvent event) {

    log.info("[Kafka Producer] PlaylistContentAddedEvent 발행 - playlistId: {}", event.playlistId());

    kafkaTemplate.send("notification-playlist-content-topic", event.playlistId().toString(), event);
  }

  /**
   * 욕설 감지 이벤트 발행
   * @param event 욕설 감지 이벤트 객체
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void produceBadWordDetectedEvent(BadWordDetectedEvent event) {
    log.info("[Kafka Producer] BadWordDetectedEvent 발행 - userId: {}", event.userId());
    kafkaTemplate.send("notification-badword-topic", event.userId().toString(), event);
  }
}
