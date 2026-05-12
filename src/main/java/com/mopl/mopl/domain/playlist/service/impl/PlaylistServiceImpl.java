package com.mopl.mopl.domain.playlist.service.impl;

import com.mopl.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.exception.PlaylistForbiddenException;
import com.mopl.mopl.domain.playlist.exception.PlaylistNotFoundException;
import com.mopl.mopl.domain.playlist.mapper.PlaylistMapper;
import com.mopl.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.mopl.domain.playlist.service.PlaylistService;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistServiceImpl implements PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final PlaylistMapper playlistMapper;

  @Override
  @Transactional
  public PlaylistDto createPlaylist(PlaylistCreateRequest request, UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(UserNotFoundException::new);

    Playlist playlist = Playlist.builder()
        .title(request.title())
        .description(request.description())
        .user(user)
        .build();

    Playlist savedPlaylist = playlistRepository.save(playlist);

    return playlistMapper.toDto(savedPlaylist);
  }

  @Override
  public PlaylistDto findPlaylistById(UUID playlistId, UUID currentUserId) {
    // TODO: 구현 예정
    return null;
  }

  @Override
  @Transactional
  public PlaylistDto updatePlaylist(UUID playlistId, PlaylistUpdateRequest request, UUID userId) {
    Playlist playlist = findPlaylistAndCheckOwner(playlistId, userId);

    playlist.update(request.title(), request.description());

    return playlistMapper.toDto(playlist);
  }

  @Override
  @Transactional
  public void deletePlaylist(UUID playlistId, UUID userId) {
    Playlist playlist = findPlaylistAndCheckOwner(playlistId, userId);

    playlistRepository.delete(playlist);
  }

  private Playlist findPlaylistAndCheckOwner(UUID playlistId, UUID userId) {

    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));
    if (!playlist.getUser().getId().equals(userId)) {
      throw new PlaylistForbiddenException();
    }

    return playlist;
  }
}