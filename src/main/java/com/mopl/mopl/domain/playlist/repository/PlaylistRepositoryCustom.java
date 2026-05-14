package com.mopl.mopl.domain.playlist.repository;

import com.mopl.mopl.domain.playlist.dto.request.PlaylistSearchRequest;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import org.springframework.data.domain.Slice;

public interface PlaylistRepositoryCustom {
  Slice<Playlist> findPlaylists(PlaylistSearchRequest request);

  long countPlaylists(PlaylistSearchRequest request);
}