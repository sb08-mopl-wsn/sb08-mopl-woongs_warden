package com.mopl.mopl.global.event;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.user.entity.User;

public record PlaylistSubscribedEvent(
    Playlist playlist,
    User subscriber
) {

  public static PlaylistSubscribedEvent of(Playlist playlist, User subscriber) {
    return new PlaylistSubscribedEvent(playlist, subscriber);
  }

}
