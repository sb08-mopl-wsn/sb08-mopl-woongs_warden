package com.mopl.mopl.domain.playlist.exception;

public class PlaylistDuplicateSubscriptionException extends PlaylistException {
  public PlaylistDuplicateSubscriptionException() {
    super(PlaylistErrorCode.PLAYLIST_DUPLICATE_SUBSCRIPTION);
  }
}
