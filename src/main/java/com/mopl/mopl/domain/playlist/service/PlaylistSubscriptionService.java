package com.mopl.mopl.domain.playlist.service;

import java.util.UUID;

public interface PlaylistSubscriptionService {

  //플리 구독
  void subscribeToPlaylist(UUID playlistId, UUID userId);

  //플리 구취
  void unsubscribeFromPlaylist(UUID playlistId, UUID userId);
}