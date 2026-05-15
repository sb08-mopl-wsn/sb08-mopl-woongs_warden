package com.mopl.mopl.domain.playlist.exception;

public class PlaylistCursorException extends PlaylistException {

  public PlaylistCursorException() {
    super(PlaylistErrorCode.PLAYLIST_INVALID_CURSOR);
  }
}