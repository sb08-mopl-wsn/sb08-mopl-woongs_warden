package com.mopl.mopl.domain.playlist.service;

import com.mopl.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.mopl.domain.playlist.dto.response.PlaylistDto;
import java.util.UUID;

public interface PlaylistService {

  PlaylistDto createPlaylist(PlaylistCreateRequest request, UUID userId);

  PlaylistDto findPlaylistById(UUID playlistId, UUID currentUserId);

  PlaylistDto updatePlaylist(UUID playlistId, PlaylistUpdateRequest request, UUID userId);

  void deletePlaylist(UUID playlistId, UUID userId);
}