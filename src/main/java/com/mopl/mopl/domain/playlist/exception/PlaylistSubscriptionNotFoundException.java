package com.mopl.mopl.domain.playlist.exception;

public class PlaylistSubscriptionNotFoundException extends PlaylistException {

  public PlaylistSubscriptionNotFoundException() {
    super(PlaylistErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND);
  }
}