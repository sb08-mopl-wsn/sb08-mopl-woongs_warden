package com.mopl.mopl.domain.playlist.exception;

public class PlaylistSelfSubscriptionException extends PlaylistException {

  public PlaylistSelfSubscriptionException() {
    super(PlaylistErrorCode.PLAYLIST_SELF_SUBSCRIPTION_NOT_ALLOWED);
  }
}