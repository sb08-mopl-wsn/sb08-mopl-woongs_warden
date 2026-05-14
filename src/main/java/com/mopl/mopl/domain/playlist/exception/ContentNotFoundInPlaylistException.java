package com.mopl.mopl.domain.playlist.exception;

public class ContentNotFoundInPlaylistException extends PlaylistException {
  public ContentNotFoundInPlaylistException() {
    super(PlaylistErrorCode.CONTENT_NOT_FOUND_IN_PLAYLIST);
  }
}