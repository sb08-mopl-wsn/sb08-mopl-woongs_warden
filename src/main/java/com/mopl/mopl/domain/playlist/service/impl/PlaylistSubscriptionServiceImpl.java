package com.mopl.mopl.domain.playlist.service.impl;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistSubscription;
import com.mopl.mopl.domain.playlist.exception.PlaylistDuplicateSubscriptionException;
import com.mopl.mopl.domain.playlist.exception.PlaylistNotFoundException;
import com.mopl.mopl.domain.playlist.exception.PlaylistSelfSubscriptionException;
import com.mopl.mopl.domain.playlist.exception.PlaylistSubscriptionNotFoundException;
import com.mopl.mopl.domain.playlist.exception.PlaylistUpdateFailedException;
import com.mopl.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.mopl.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.mopl.mopl.domain.playlist.service.PlaylistSubscriptionService;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import com.mopl.mopl.global.event.PlaylistSubscribedEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistSubscriptionServiceImpl implements PlaylistSubscriptionService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public void subscribeToPlaylist(UUID playlistId, UUID userId) {
    Playlist playlist = playlistRepository.findByIdWithUserForUpdate(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));
    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    if (playlist.getUser().equals(user)) {
      throw new PlaylistSelfSubscriptionException();
    }

    try {
      PlaylistSubscription subscription = new PlaylistSubscription(user, playlist);
      playlistSubscriptionRepository.saveAndFlush(subscription);
    } catch (DataIntegrityViolationException e) {
      throw new PlaylistDuplicateSubscriptionException();
    }

    int updatedRows = playlistRepository.increaseSubscriberCount(playlistId);
    if (updatedRows == 0) {
      throw new PlaylistUpdateFailedException();
    }

    eventPublisher.publishEvent(PlaylistSubscribedEvent.of(playlist, user));
  }

  @Override
  @Transactional
  public void unsubscribeFromPlaylist(UUID playlistId, UUID userId) {
    Playlist playlist = playlistRepository.findByIdWithUserForUpdate(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));
    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    PlaylistSubscription subscription = playlistSubscriptionRepository
        .findByUserAndPlaylist(user, playlist)
        .orElseThrow(PlaylistSubscriptionNotFoundException::new);

    playlistSubscriptionRepository.delete(subscription);

    playlistSubscriptionRepository.flush();

    int updatedRows = playlistRepository.decreaseSubscriberCount(playlistId);
    if (updatedRows == 0) {
      throw new PlaylistUpdateFailedException();
    }
  }
}