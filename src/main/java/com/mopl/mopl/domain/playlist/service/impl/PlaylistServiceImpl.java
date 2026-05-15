package com.mopl.mopl.domain.playlist.service.impl;

import com.mopl.mopl.domain.content.dto.response.ContentSummary;
import com.mopl.mopl.domain.content.mapper.ContentMapper;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistSearchRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.mopl.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.mopl.domain.playlist.exception.PlaylistForbiddenException;
import com.mopl.mopl.domain.playlist.exception.PlaylistNotFoundException;
import com.mopl.mopl.domain.playlist.mapper.PlaylistMapper;
import com.mopl.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.mopl.domain.playlist.repository.PlaylistSubscriptionRepository;
import com.mopl.mopl.domain.playlist.service.PlaylistService;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistServiceImpl implements PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final PlaylistMapper playlistMapper;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final PlaylistContentRepository playlistContentRepository;
  private final ContentMapper contentMapper;

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
    Playlist playlist = playlistRepository.findById(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));

    boolean isSubscribed = false;
    if (currentUserId != null) {
      isSubscribed = playlistSubscriptionRepository.existsByUserIdAndPlaylistId(currentUserId, playlistId);
    }

    List<PlaylistContent> playlistContents = playlistContentRepository.findAllByPlaylistId(playlistId);
    List<ContentSummary> contents = playlistContents.stream()
        .map(pc -> contentMapper.toContentSummary(pc.getContent()))
        .toList();

    return playlistMapper.toDto(playlist, isSubscribed, contents);

  }

  @Override
  public CursorResponsePlaylistDto findPlaylists(PlaylistSearchRequest request) {
    Slice<Playlist> slice = playlistRepository.findPlaylists(request);

    List<UUID> playlistIds = slice.getContent().stream()
        .map(Playlist::getId)
        .toList();

    Map<UUID, List<ContentSummary>> contentsByPlaylistId = playlistContentRepository
        .findAllByPlaylistIdIn(playlistIds).stream()
        .collect(Collectors.groupingBy(
            pc -> pc.getPlaylist().getId(),
            Collectors.mapping(pc -> contentMapper.toContentSummary(pc.getContent()), Collectors.toList())
        ));

    List<PlaylistDto> data = slice.getContent().stream()
        .map(playlist -> playlistMapper.toDto(
            playlist,
            false,
            contentsByPlaylistId.getOrDefault(playlist.getId(), Collections.emptyList())
        ))
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (slice.hasNext() && !data.isEmpty()) {
      Playlist lastPlaylist = slice.getContent().get(slice.getContent().size() - 1);
      nextIdAfter = lastPlaylist.getId();

      String sortBy = request.sortBy() != null ? request.sortBy().toLowerCase() : "updatedat";

      if ("subscribercount".equals(sortBy)) {
        nextCursor = String.valueOf(lastPlaylist.getSubscriberCount());
      } else {
        // 기본값: updatedAt (최신순)
        nextCursor = lastPlaylist.getUpdatedAt().toString();
      }
    }
    long totalCount = playlistRepository.countPlaylists(request);

    return new CursorResponsePlaylistDto(
        data,
        nextCursor,
        nextIdAfter,
        slice.hasNext(),
        totalCount,
        request.sortBy(),
        request.sortDirection()
    );
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

    playlistContentRepository.deleteAllByPlaylistId(playlistId);
    playlistSubscriptionRepository.deleteAllByPlaylistId(playlistId);

    playlistRepository.delete(playlist);
  }

  private Playlist findPlaylistAndCheckOwner(UUID playlistId, UUID userId) {

    Playlist playlist = playlistRepository.findByIdWithUser(playlistId)
        .orElseThrow(() -> new PlaylistNotFoundException(playlistId));
    if (!playlist.getUser().getId().equals(userId)) {
      throw new PlaylistForbiddenException();
    }

    return playlist;
  }
}