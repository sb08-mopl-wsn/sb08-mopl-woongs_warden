package com.mopl.mopl.domain.playlist.controller;

import com.mopl.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.mopl.domain.playlist.service.PlaylistService;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

  private final PlaylistService playlistService;

  @PostMapping
  public ResponseEntity<PlaylistDto> createPlaylist(
      @Valid @RequestBody PlaylistCreateRequest request,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    PlaylistDto responseDto = playlistService.createPlaylist(request, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
  }

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

  @DeleteMapping("/{playlistId}")
  public ResponseEntity<Void> deletePlaylist(
      @PathVariable UUID playlistId,
      @AuthenticationPrincipal MoplUserDetails userDetails
  ) {
    UUID userId = userDetails.getUserDto().id();
    playlistService.deletePlaylist(playlistId, userId);
    return ResponseEntity.noContent().build();
  }
}