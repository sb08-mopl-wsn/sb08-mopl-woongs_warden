package com.mopl.mopl.domain.notification.service.kafka;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.event.*;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaProducerTest {

  @InjectMocks
  private NotificationKafkaProducer notificationKafkaProducer;

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Test
  @DisplayName("팔로우 이벤트 발생 시 카프카 해당 토픽으로 메시지를 전송한다.")
  void produceFollowEvent_SendsToKafka() {

    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    User follower = User.builder().name("팔로워").build();
    ReflectionTestUtils.setField(follower, "id", followerId);
    User followee = User.builder().name("수신자").build();
    ReflectionTestUtils.setField(followee, "id", followeeId);
    FollowEvent event = FollowEvent.of(follower, followee);

    // when
    notificationKafkaProducer.produceFollowEvent(event);

    // then
    verify(kafkaTemplate).send(eq("notification-follow-topic"), eq(followeeId.toString()), eq(event));
  }

  @Test
  @DisplayName("리뷰 작성 이벤트 발생 시 카프카 해당 토픽으로 메시지를 전송한다.")
  void produceReviewEvent_SendsToKafka() {

    // given
    UUID writerId = UUID.randomUUID();
    User writer = User.builder().name("작성자").build();
    ReflectionTestUtils.setField(writer, "id", writerId);

    Review review = Review.builder().user(writer).build();
    ReflectionTestUtils.setField(review, "id", UUID.randomUUID());

    ReviewCreatedEvent event = ReviewCreatedEvent.of(review);

    // when
    notificationKafkaProducer.produceReviewEvent(event);

    // then
    verify(kafkaTemplate).send(eq("notification-review-topic"), eq(writerId.toString()), eq(event));
  }

  @Test
  @DisplayName("DM 알림 이벤트 발생 시 카프카 해당 토픽으로 메시지를 전송한다.")
  void produceDirectMessageEvent_SendsToKafka() {

    // given
    UUID conversationId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    UserSummary senderSummary = new UserSummary(UUID.randomUUID(), "보낸이", null);
    DirectMessageDto messageDto = new DirectMessageDto(UUID.randomUUID(), conversationId, "안녕", senderSummary, null,
        Instant.now());

    DirectMessageCreatedEvent event = new DirectMessageCreatedEvent(conversationId, receiverId, messageDto);

    // when
    notificationKafkaProducer.produceDirectMessageEvent(event);

    // then
    verify(kafkaTemplate).send(eq("notification-dm-topic"), eq(receiverId.toString()), eq(event));
  }

  @Test
  @DisplayName("권한 변경 이벤트 발생 시 카프카 해당 토픽으로 메시지를 전송한다.")
  void producerUserUpdatedRoleEvent_SendsToKafka() {

    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRoleEvent event = new UserUpdateRoleEvent(userId, "유저", Role.ADMIN);

    // when
    notificationKafkaProducer.produceUserUpdateRoleEvent(event);

    // then
    verify(kafkaTemplate).send(eq("notification-role-topic"), eq(userId.toString()), eq(event));
  }

  @Test
  @DisplayName("플레이리스트 구독 이벤트 발생 시 카프카 해당 토픽으로 메시지를 전송한다.")
  void producePlaylistSubscribedEvent_SendsToKafka() {

    // given
    UUID ownerId = UUID.randomUUID();
    User owner = User.builder().name("주인장").build();
    ReflectionTestUtils.setField(owner, "id", ownerId);

    Playlist playlist = Playlist.builder().user(owner).title("내플리").build();
    ReflectionTestUtils.setField(playlist, "id", UUID.randomUUID());

    User subscriber = User.builder().name("구독자").build();
    PlaylistSubscribedEvent event = PlaylistSubscribedEvent.of(playlist, subscriber);

    // when
    notificationKafkaProducer.producePlaylistSubscribedEvent(event);

    // then
    verify(kafkaTemplate).send(eq("notification-playlist-subscribe-topic"), eq(ownerId.toString()), eq(event));
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 이벤트 발생 시 카프카 해당 토픽으로 메시지를 전송한다.")
  void producePlaylistContentAddedEvent_SendsToKafka() {

    // given
    UUID playlistId = UUID.randomUUID();
    Playlist playlist = Playlist.builder().user(User.builder().build()).title("내플리").build();
    ReflectionTestUtils.setField(playlist, "id", playlistId);

    PlaylistContentAddedEvent event = PlaylistContentAddedEvent.of(playlist);

    // when
    notificationKafkaProducer.producePlaylistContentAddedEvent(event);

    // then
    verify(kafkaTemplate).send(eq("notification-playlist-content-topic"), eq(playlistId.toString()), eq(event));
  }

  @Test
  @DisplayName("비속어 감지 이벤트 발생 시 카프카 해당 토픽으로 메시지를 전송한다.")
  void produceBadWordDetectedEvent_SendsToKafka() {

    // given
    UUID userId = UUID.randomUUID();
    String content = "바보";
    BadWordDetectedEvent event = new BadWordDetectedEvent(userId, content);

    // when
    notificationKafkaProducer.produceBadWordDetectedEvent(event);

    // then
    verify(kafkaTemplate).send(eq("notification-badWord-topic"), eq(userId.toString()), eq(event));
  }
}