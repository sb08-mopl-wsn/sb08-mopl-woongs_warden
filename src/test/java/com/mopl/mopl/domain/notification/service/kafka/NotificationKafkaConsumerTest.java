package com.mopl.mopl.domain.notification.service.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.mopl.mopl.domain.dm.dto.DirectMessageDto;
import com.mopl.mopl.domain.dm.service.RoomPresenceManager;
import com.mopl.mopl.domain.follow.entity.Follow;
import com.mopl.mopl.domain.follow.repository.FollowRepository;
import com.mopl.mopl.domain.notification.dto.NotificationDto;
import com.mopl.mopl.domain.notification.entity.Notification;
import com.mopl.mopl.domain.notification.mapper.NotificationMapper;
import com.mopl.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistSubscription;
import com.mopl.mopl.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.mopl.mopl.domain.review.entity.Review;
import com.mopl.mopl.domain.user.dto.UserSummary;
import com.mopl.mopl.domain.user.entity.Role;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.DirectMessageCreatedEvent;
import com.mopl.mopl.global.event.FollowEvent;
import com.mopl.mopl.global.event.PlaylistContentAddedEvent;
import com.mopl.mopl.global.event.PlaylistSubscribedEvent;
import com.mopl.mopl.global.event.ReviewCreatedEvent;
import com.mopl.mopl.global.event.user.UserUpdateRoleEvent;
import com.mopl.mopl.global.sse.service.SseService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationKafkaConsumerTest {

  @InjectMocks
  private NotificationKafkaConsumer notificationKafkaConsumer;

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private FollowRepository followRepository;
  @Mock
  private PlaylistSubscriptionRepository playlistSubscriptionRepository;
  @Mock
  private SseService sseService;
  @Mock
  private NotificationMapper notificationMapper;
  @Mock
  private RoomPresenceManager roomPresenceManager;

  private UUID receiverId;
  private User receiver;
  private Notification mockNotification;
  private NotificationDto mockDto;

  @BeforeEach
  void setUp() {
    receiverId = UUID.randomUUID();
    receiver = User.builder().email("receiver@test.com").name("수신자").build();
    ReflectionTestUtils.setField(receiver, "id", receiverId);

    mockNotification = Notification.builder().user(receiver).title("Test").content("Content").build();
    mockDto = new NotificationDto(UUID.randomUUID(), "Test", "Content", null, receiverId, null);
  }

  @Test
  @DisplayName("팔로우 이벤트 수신 시 알림을 저장하고 SSE를 발송한다.")
  void handleFollowEvent() {

    // given
    User follower = User.builder().name("팔로워").build();
    ReflectionTestUtils.setField(follower, "id", UUID.randomUUID());

    FollowEvent event = FollowEvent.of(follower, receiver);

    given(userRepository.getReferenceById(receiverId)).willReturn(receiver);
    given(notificationRepository.save(any(Notification.class))).willReturn(mockNotification);
    given(notificationMapper.toDto(any())).willReturn(mockDto);

    // when
    notificationKafkaConsumer.consumeFollowEvent(event);

    // then
    verify(notificationRepository).save(any(Notification.class));
    verify(sseService).sendNotification(eq(receiverId), eq(mockDto));
  }

  @Test
  @DisplayName("DM 수신 시 유저가 채팅방 밖에 있으면 알림을 발송한다.")
  void handleDirectMessageEvent_NotInRoom() {

    // given
    UUID convId = UUID.randomUUID();
    UserSummary senderSummary = new UserSummary(UUID.randomUUID(), "보낸이", null);
    DirectMessageDto messageDto = new DirectMessageDto(UUID.randomUUID(), convId,"안녕", senderSummary, null,
        Instant.now());
    DirectMessageCreatedEvent event = DirectMessageCreatedEvent.of(convId, receiverId, messageDto);

    given(userRepository.getReferenceById(receiverId)).willReturn(receiver);
    given(roomPresenceManager.isUserInRoom(receiverId, convId)).willReturn(false);
    given(notificationRepository.save(any(Notification.class))).willReturn(mockNotification);
    given(notificationMapper.toDto(any())).willReturn(mockDto);

    // when
    notificationKafkaConsumer.consumeDirectMessageEvent(event);

    // then
    verify(notificationRepository).save(any(Notification.class));
    verify(sseService).sendNotification(eq(receiverId), eq(mockDto));
  }

  @Test
  @DisplayName("권한 변경 이벤트 수신 시 알림을 저장하고 발송한다.")
  void handleUserUpdateRoleEvent() {

    // given
    UserUpdateRoleEvent event = new UserUpdateRoleEvent(receiverId, "수신자", Role.ADMIN);
    given(userRepository.getReferenceById(receiverId)).willReturn(receiver);
    given(notificationRepository.save(any(Notification.class))).willReturn(mockNotification);
    given(notificationMapper.toDto(any())).willReturn(mockDto);

    // when
    notificationKafkaConsumer.consumeUserUpdateRoleEvent(event);

    // then
    verify(notificationRepository).save(any(Notification.class));
    verify(sseService).sendNotification(eq(receiverId), eq(mockDto));
  }

  @Test
  @DisplayName("리뷰 작성 시 팔로워가 있으면 브로드캐스팅(saveAll)이 동작한다.")
  void handleReviewCreatedEvent() {

    // given
    User writer = User.builder().name("작성자").build();
    UUID writerId = UUID.randomUUID();
    ReflectionTestUtils.setField(writer, "id", writerId);

    Review review = Review.builder().user(writer).build();
    ReflectionTestUtils.setField(review, "id", UUID.randomUUID());

    ReviewCreatedEvent event = ReviewCreatedEvent.of(review);

    Follow mockFollow = Follow.builder().follower(receiver).followee(writer).build();
    given(followRepository.findAllByFolloweeIdWithFollower(writerId)).willReturn(List.of(mockFollow));
    given(notificationRepository.saveAll(any())).willReturn(List.of(mockNotification));
    given(notificationMapper.toDto(any())).willReturn(mockDto);

    // when
    notificationKafkaConsumer.consumeReviewEvent(event);

    // then
    verify(notificationRepository).saveAll(any());
    verify(sseService).sendNotification(eq(receiverId), eq(mockDto));
  }

  @Test
  @DisplayName("내 플레이리스트를 누군가 구독했을 때 알림을 저장하고 발송한다.")
  void handlePlaylistSubscribedEvent() {

    // given
    Playlist playlist = Playlist.builder().user(receiver).title("내플리").build();
    ReflectionTestUtils.setField(playlist, "id", UUID.randomUUID());
    User subscriber = User.builder().name("구독자").build();

    PlaylistSubscribedEvent event = PlaylistSubscribedEvent.of(playlist, subscriber);

    given(userRepository.getReferenceById(receiverId)).willReturn(receiver);
    given(notificationRepository.save(any(Notification.class))).willReturn(mockNotification);
    given(notificationMapper.toDto(any())).willReturn(mockDto);

    // when
    notificationKafkaConsumer.consumePlaylistSubscribedEvent(event);

    // then
    verify(notificationRepository).save(any(Notification.class));
    verify(sseService).sendNotification(eq(receiverId), eq(mockDto));
  }

  @Test
  @DisplayName("구독 중인 플레이리스트 콘텐츠 추가 시 브로드캐스팅이 동작한다.")
  void handlePlaylistContentAddedEvent() {

    // given
    Playlist playlist = Playlist.builder().user(User.builder().build()).title("업데이트플리").build();
    UUID playlistId = UUID.randomUUID();
    ReflectionTestUtils.setField(playlist, "id", playlistId);

    PlaylistContentAddedEvent event = PlaylistContentAddedEvent.of(playlist);
    PlaylistSubscription mockSub = PlaylistSubscription.builder().user(receiver).playlist(playlist).build();

    given(playlistSubscriptionRepository.findAllByPlaylistIdWithUser(playlistId)).willReturn(List.of(mockSub));
    given(notificationRepository.saveAll(any())).willReturn(List.of(mockNotification));
    given(notificationMapper.toDto(any())).willReturn(mockDto);

    // when
    notificationKafkaConsumer.consumePlaylistContentAddedEvent(event);

    // then
    verify(notificationRepository).saveAll(any());
    verify(sseService).sendNotification(eq(receiverId), eq(mockDto));
  }

  @Test
  @DisplayName("DM 수신 시 유저가 이미 채팅방 안에 있으면 알림을 저장하거나 발송하지 않는다.")
  void handleDirectMessageEvent_InRoom() {

    // given
    UUID convId = UUID.randomUUID();
    UserSummary senderSummary = new UserSummary(UUID.randomUUID(), "보낸이", null);
    DirectMessageDto messageDto = new DirectMessageDto(UUID.randomUUID(), convId, "안녕", senderSummary, null, Instant.now());
    DirectMessageCreatedEvent event = DirectMessageCreatedEvent.of(convId, receiverId, messageDto);

    given(roomPresenceManager.isUserInRoom(receiverId, convId)).willReturn(true);

    // when
    notificationKafkaConsumer.consumeDirectMessageEvent(event);

    // then
    verify(notificationRepository, never()).save(any(Notification.class));
    verify(sseService, never()).sendNotification(any(), any());
  }

  @Test
  @DisplayName("리뷰 작성 시 팔로워가 아무도 없으면 브로드캐스팅 로직을 조용히 건너뛴다.")
  void handleReviewCreatedEvent_NoFollowers() {

    // given
    User writer = User.builder().name("작성자").build();
    UUID writerId = UUID.randomUUID();
    ReflectionTestUtils.setField(writer, "id", writerId);

    Review review = Review.builder().user(writer).build();
    ReflectionTestUtils.setField(review, "id", UUID.randomUUID());

    ReviewCreatedEvent event = ReviewCreatedEvent.of(review);

    given(followRepository.findAllByFolloweeIdWithFollower(writerId)).willReturn(Collections.emptyList());

    // when
    notificationKafkaConsumer.consumeReviewEvent(event);

    // then
    verify(notificationRepository, never()).saveAll(any());
    verify(sseService, never()).sendNotification(any(), any());
  }

  @Test
  @DisplayName("구독 중인 플레이리스트에 콘텐츠가 추가되어도 구독자가 없으면 브로드캐스팅을 건너뛴다.")
  void handlePlaylistContentAddedEvent_NoSubscribers() {

    // given
    Playlist playlist = Playlist.builder().user(User.builder().build()).title("업데이트플리").build();
    UUID playlistId = UUID.randomUUID();
    ReflectionTestUtils.setField(playlist, "id", playlistId);

    PlaylistContentAddedEvent event = PlaylistContentAddedEvent.of(playlist);

    given(playlistSubscriptionRepository.findAllByPlaylistIdWithUser(playlistId)).willReturn(Collections.emptyList());

    // when
    notificationKafkaConsumer.consumePlaylistContentAddedEvent(event);

    // then
    verify(notificationRepository, never()).saveAll(any());
    verify(sseService, never()).sendNotification(any(), any());
  }
}