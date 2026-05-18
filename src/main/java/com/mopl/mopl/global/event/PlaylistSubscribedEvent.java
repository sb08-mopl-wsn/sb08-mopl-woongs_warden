package com.mopl.mopl.global.event;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.user.entity.User;
import java.util.UUID;

public record PlaylistSubscribedEvent(
    UUID playlistOwnerId,
    String playlistTitle,
    String subscriberName
) {

  public static PlaylistSubscribedEvent of(Playlist playlist, User subscriber) {
    return new PlaylistSubscribedEvent(playlist.getUser().getId(), playlist.getTitle(), subscriber.getName());
  }

}
