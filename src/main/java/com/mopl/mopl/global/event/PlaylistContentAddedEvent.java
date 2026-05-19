package com.mopl.mopl.global.event;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import java.util.UUID;

public record PlaylistContentAddedEvent(
    UUID playlistId,
    String playlistTitle
) {

  public static PlaylistContentAddedEvent of(Playlist playlist) {
    return new PlaylistContentAddedEvent(playlist.getId(), playlist.getTitle());
  }
}
