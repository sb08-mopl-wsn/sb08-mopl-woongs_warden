package com.mopl.mopl.domain.playlist.exception;

import java.util.UUID;

public class PlaylistNotFoundException extends PlaylistException {

  public PlaylistNotFoundException() {
    super(PlaylistErrorCode.PLAYLIST_NOT_FOUND);
  }

  public PlaylistNotFoundException(UUID playlistId) {
    super(PlaylistErrorCode.PLAYLIST_NOT_FOUND, PlaylistErrorCode.PLAYLIST_NOT_FOUND.getMessage() + " id=" + playlistId);
  }
}