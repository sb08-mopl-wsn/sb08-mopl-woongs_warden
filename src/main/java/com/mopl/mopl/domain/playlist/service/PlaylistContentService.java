package com.mopl.mopl.domain.playlist.service;

import java.util.UUID;

public interface PlaylistContentService {

  //플리에 콘텐츠 추가
  void addContentToPlaylist(UUID playlistId, UUID contentId, UUID userId);

  //플리에서 콘텐츠 삭제
  void removeContentFromPlaylist(UUID playlistId, UUID contentId, UUID userId);
}