package com.mopl.mopl.domain.playlist.exception;

import java.util.UUID;

public class PlaylistNotFoundException extends PlaylistException {

  public PlaylistNotFoundException() {
    super(PlaylistErrorCode.PLAYLIST_NOT_FOUND);
  }

  public PlaylistNotFoundException(UUID playlistId) {
    super(PlaylistErrorCode.PLAYLIST_NOT_FOUND, "ID가 " + playlistId + "인 플레이리스트를 찾을 수 없습니다.");
  }
}