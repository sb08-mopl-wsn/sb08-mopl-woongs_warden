package com.mopl.mopl.domain.playlist.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistSubscription;
import com.mopl.mopl.domain.playlist.exception.PlaylistDuplicateSubscriptionException;
import com.mopl.mopl.domain.playlist.exception.PlaylistNotFoundException;
import com.mopl.mopl.domain.playlist.exception.PlaylistSelfSubscriptionException;
import com.mopl.mopl.domain.playlist.exception.PlaylistSubscriptionNotFoundException;
import com.mopl.mopl.domain.playlist.exception.PlaylistUpdateFailedException;
import com.mopl.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.mopl.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.mopl.mopl.domain.playlist.service.impl.PlaylistSubscriptionServiceImpl;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.PlaylistSubscribedEvent;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaylistSubscriptionService Unit Test")
class PlaylistSubscriptionServiceImplTest {

  @InjectMocks
  private PlaylistSubscriptionServiceImpl playlistSubscriptionService;

  @Mock
  private PlaylistRepository playlistRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private PlaylistSubscriptionRepository playlistSubscriptionRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private User owner;
  private User subscriber;
  private Playlist playlist;
  private UUID playlistId;
  private UUID ownerId;
  private UUID subscriberId;

  @BeforeEach
  void setUp() {
    ownerId = UUID.randomUUID();
    subscriberId = UUID.randomUUID();
    playlistId = UUID.randomUUID();

    owner = User.builder().name("owner").email("owner@test.com").build();
    ReflectionTestUtils.setField(owner, "id", ownerId);

    subscriber = User.builder().name("subscriber").email("sub@test.com").build();
    ReflectionTestUtils.setField(subscriber, "id", subscriberId);

    playlist = Playlist.builder().user(owner).title("test playlist").description("desc").build();
    ReflectionTestUtils.setField(playlist, "id", playlistId);
  }

  @Nested
  @DisplayName("플레이리스트 구독")
  class Subscribe {

    @Test
    @DisplayName("정상적으로 플레이리스트를 구독하고 카운트를 증가시킨다.")
    void givenValidRequest_whenSubscribe_thenSuccess() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
      given(playlistRepository.increaseSubscriberCount(playlistId)).willReturn(1);

      // when
      playlistSubscriptionService.subscribeToPlaylist(playlistId, subscriberId);

      // then
      then(playlistSubscriptionRepository).should().saveAndFlush(any(PlaylistSubscription.class));
      then(playlistRepository).should().increaseSubscriberCount(playlistId);
      then(eventPublisher).should().publishEvent(any(PlaylistSubscribedEvent.class));
    }

    @Test
    @DisplayName("자신의 플레이리스트를 구독하면 예외가 발생한다.")
    void givenOwnPlaylist_whenSubscribe_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));

      // when & then
      assertThatThrownBy(() -> playlistSubscriptionService.subscribeToPlaylist(playlistId, ownerId))
          .isInstanceOf(PlaylistSelfSubscriptionException.class);
    }

    @Test
    @DisplayName("이미 구독한 플레이리스트를 다시 구독하면 예외가 발생한다.")
    void givenAlreadySubscribed_whenSubscribe_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
      given(playlistSubscriptionRepository.saveAndFlush(any(PlaylistSubscription.class)))
          .willThrow(new DataIntegrityViolationException("duplicate"));

      // when & then
      assertThatThrownBy(
          () -> playlistSubscriptionService.subscribeToPlaylist(playlistId, subscriberId))
          .isInstanceOf(PlaylistDuplicateSubscriptionException.class);
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트를 구독하려 하면 예외가 발생한다.")
    void givenNonExistingPlaylist_whenSubscribe_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> playlistSubscriptionService.subscribeToPlaylist(playlistId, subscriberId))
          .isInstanceOf(PlaylistNotFoundException.class);
    }

    @Test
    @DisplayName("카운트 증가 쿼리가 실패하면 예외가 발생한다.")
    void givenIncreaseCountFails_whenSubscribe_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));

      given(playlistRepository.increaseSubscriberCount(playlistId)).willReturn(0);

      // when & then
      assertThatThrownBy(
          () -> playlistSubscriptionService.subscribeToPlaylist(playlistId, subscriberId))
          .isInstanceOf(PlaylistUpdateFailedException.class);
    }
  }

  @Nested
  @DisplayName("플레이리스트 구독 취소")
  class Unsubscribe {

    @Test
    @DisplayName("정상적으로 구독을 취소하고 카운트를 감소시킨다.")
    void givenValidRequest_whenUnsubscribe_thenSuccess() {
      // given
      PlaylistSubscription subscription = new PlaylistSubscription(subscriber, playlist);

      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
      given(playlistSubscriptionRepository.findByUserAndPlaylist(subscriber, playlist))
          .willReturn(Optional.of(subscription));
      given(playlistRepository.decreaseSubscriberCount(playlistId)).willReturn(1);

      // when
      playlistSubscriptionService.unsubscribeFromPlaylist(playlistId, subscriberId);

      // then
      then(playlistSubscriptionRepository).should().delete(subscription);
      then(playlistSubscriptionRepository).should().flush();
      then(playlistRepository).should().decreaseSubscriberCount(playlistId);
    }

    @Test
    @DisplayName("구독 내역이 존재하지 않으면 예외가 발생한다.")
    void givenNotSubscribed_whenUnsubscribe_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
      given(playlistSubscriptionRepository.findByUserAndPlaylist(subscriber, playlist))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> playlistSubscriptionService.unsubscribeFromPlaylist(playlistId, subscriberId))
          .isInstanceOf(PlaylistSubscriptionNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트의 구독을 취소하려 하면 예외가 발생한다.")
    void givenNonExistingPlaylist_whenUnsubscribe_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(
          () -> playlistSubscriptionService.unsubscribeFromPlaylist(playlistId, subscriberId))
          .isInstanceOf(PlaylistNotFoundException.class);
    }

    @Test
    @DisplayName("카운트 감소 쿼리가 실패하면 예외가 발생한다.")
    void givenDecreaseCountFails_whenUnsubscribe_thenThrowsException() {
      // given
      PlaylistSubscription subscription = new PlaylistSubscription(subscriber, playlist);

      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(userRepository.findById(subscriberId)).willReturn(Optional.of(subscriber));
      given(playlistSubscriptionRepository.findByUserAndPlaylist(subscriber, playlist))
          .willReturn(Optional.of(subscription));

      given(playlistRepository.decreaseSubscriberCount(playlistId)).willReturn(0);

      // when & then
      assertThatThrownBy(
          () -> playlistSubscriptionService.unsubscribeFromPlaylist(playlistId, subscriberId))
          .isInstanceOf(PlaylistUpdateFailedException.class);
    }
  }
}