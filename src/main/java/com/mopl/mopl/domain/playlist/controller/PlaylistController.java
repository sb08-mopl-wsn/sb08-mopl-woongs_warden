package com.mopl.mopl.domain.playlist.controller;

import com.mopl.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistSearchRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.mopl.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.mopl.domain.playlist.service.PlaylistContentService;
import com.mopl.mopl.domain.playlist.service.PlaylistService;
import com.mopl.mopl.domain.playlist.service.PlaylistSubscriptionService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController implements PlaylistApi {

  private final PlaylistService playlistService;
  private final PlaylistContentService playlistContentService;
  private final PlaylistSubscriptionService playlistSubscriptionService;


  @Override
  @PostMapping
  public ResponseEntity<PlaylistDto> createPlaylist(
      @Valid @RequestBody PlaylistCreateRequest request,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    PlaylistDto responseDto = playlistService.createPlaylist(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

  @Override
  @GetMapping("/{playlistId}")
  public ResponseEntity<PlaylistDto> findPlaylistById(
      @PathVariable UUID playlistId,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID currentUserId = (userDetails != null) ? userDetails.getUserDto().id() : null;

    PlaylistDto responseDto = playlistService.findPlaylistById(playlistId, currentUserId);

    return ResponseEntity.ok(responseDto);
  }

  @Override
  @GetMapping
  public ResponseEntity<CursorResponsePlaylistDto> findPlaylists(
      @Valid @ParameterObject PlaylistSearchRequest request
  ) {
    CursorResponsePlaylistDto responseDto = playlistService.findPlaylists(request);
    return ResponseEntity.ok(responseDto);
  }

  @Override
  @PatchMapping("/{playlistId}")
  public ResponseEntity<PlaylistDto> updatePlaylist(
      @PathVariable UUID playlistId,
      @Valid @RequestBody PlaylistUpdateRequest request,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    PlaylistDto responseDto = playlistService.updatePlaylist(playlistId, request, userId);
    return ResponseEntity.ok(responseDto);
  }

  @Override
  @DeleteMapping("/{playlistId}")
  public ResponseEntity<Void> deletePlaylist(
      @PathVariable UUID playlistId,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    playlistService.deletePlaylist(playlistId, userId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> addContentToPlaylist(
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    playlistContentService.addContentToPlaylist(playlistId, contentId, userId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> removeContentFromPlaylist(
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    playlistContentService.removeContentFromPlaylist(playlistId, contentId, userId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PostMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> subscribeToPlaylist(
      @PathVariable UUID playlistId,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    playlistSubscriptionService.subscribeToPlaylist(playlistId, userId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @DeleteMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> unsubscribeFromPlaylist(
      @PathVariable UUID playlistId,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    playlistSubscriptionService.unsubscribeFromPlaylist(playlistId, userId);
    return ResponseEntity.noContent().build();
  }
}