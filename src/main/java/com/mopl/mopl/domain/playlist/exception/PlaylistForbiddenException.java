package com.mopl.mopl.domain.playlist.exception;

public class PlaylistForbiddenException extends PlaylistException {
  public PlaylistForbiddenException() {
    super(PlaylistErrorCode.PLAYLIST_FORBIDDEN);
  }
}
