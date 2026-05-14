package com.mopl.mopl.domain.playlist.service.impl;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.mopl.domain.playlist.exception.ContentAlreadyInPlaylistException;
import com.mopl.mopl.domain.playlist.exception.ContentNotFoundInPlaylistException;
import com.mopl.mopl.domain.playlist.exception.PlaylistForbiddenException;
import com.mopl.mopl.domain.playlist.exception.PlaylistNotFoundException;
import com.mopl.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.mopl.domain.playlist.service.PlaylistContentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service // 👈 스프링이 "아! 이게 그 구현체구나!" 하고 알 수 있게 해주는 어노테이션
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistContentServiceImpl implements PlaylistContentService {

  private final PlaylistRepository playlistRepository;
  private final ContentRepository contentRepository;
  private final PlaylistContentRepository playlistContentRepository;

  @Override
  @Transactional
  public void addContentToPlaylist(UUID playlistId, UUID contentId, UUID userId) {
    Playlist playlist = findPlaylistAndCheckOwner(playlistId, userId);
    Content content = contentRepository.findById(contentId)
        .orElseThrow(ContentNotFoundException::new);

    boolean alreadyExists = playlistContentRepository
        .findByPlaylistAndContentId(playlist, contentId).isPresent();
    if (alreadyExists) {
      throw new ContentAlreadyInPlaylistException();
    }

    PlaylistContent playlistContent = new PlaylistContent(playlist, content);
    playlistContentRepository.save(playlistContent);
    playlist.increaseContentCount();
  }

  @Override
  @Transactional
  public void removeContentFromPlaylist(UUID playlistId, UUID contentId, UUID userId) {
    Playlist playlist = findPlaylistAndCheckOwner(playlistId, userId);
    PlaylistContent playlistContent = playlistContentRepository
        .findByPlaylistAndContentId(playlist, contentId)
        .orElseThrow(ContentNotFoundInPlaylistException::new);

    playlistContentRepository.delete(playlistContent);
    playlist.decreaseContentCount();
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