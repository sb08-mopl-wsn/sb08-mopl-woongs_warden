package com.mopl.mopl.domain.playlist.exception;

public class ContentAlreadyInPlaylistException extends PlaylistException {
  public ContentAlreadyInPlaylistException() {
    super(PlaylistErrorCode.CONTENT_ALREADY_IN_PLAYLIST);
  }
}