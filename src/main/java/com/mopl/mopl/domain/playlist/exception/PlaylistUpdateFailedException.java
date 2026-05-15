package com.mopl.mopl.domain.playlist.exception;

public class PlaylistUpdateFailedException extends PlaylistException {
  public PlaylistUpdateFailedException() {

    super(PlaylistErrorCode.PLAYLIST_UPDATE_FAILED);
  }
}