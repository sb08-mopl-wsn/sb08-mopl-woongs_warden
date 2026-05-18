package com.mopl.mopl.global.event;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.playlist.entity.Playlist;

public record PlaylistContentAddedEvent(
    Playlist playlist,
    Content content
) {

  public static PlaylistContentAddedEvent of(Playlist playlist, Content content) {
    return new PlaylistContentAddedEvent(playlist, content);
  }
}
