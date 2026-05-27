package com.mopl.mopl.domain.playlist.controller;

import com.mopl.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistSearchRequest;
import com.mopl.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.mopl.domain.playlist.dto.response.CursorResponsePlaylistDto;
import com.mopl.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.mopl.global.auth.details.MoplUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@Tag(name = "플레이리스트 관리", description = "플레이리스트 API")
public interface PlaylistApi {

  @Operation(summary = "플레이리스트 생성", description = "생성한 플레이리스트는 API 요청자 본인의 플레이리스트로 생성됩니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaylistDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<PlaylistDto> createPlaylist(
      @Valid @RequestBody PlaylistCreateRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  @Operation(summary = "플레이리스트 단건 조회")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaylistDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "해당 리소스 없음", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<PlaylistDto> findPlaylistById(
      @Parameter(description = "플레이리스트 UUID") @PathVariable UUID playlistId,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  @Operation(summary = "플레이리스트 목록 조회 (커서 페이지네이션)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CursorResponsePlaylistDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<CursorResponsePlaylistDto> findPlaylists(
      @ParameterObject @Valid PlaylistSearchRequest request
  );

  @Operation(summary = "플레이리스트 수정", description = "플레이리스트 소유자만 수정할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaylistDto.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "권한 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<PlaylistDto> updatePlaylist(
      @Parameter(description = "플레이리스트 UUID") @PathVariable UUID playlistId,
      @Valid @RequestBody PlaylistUpdateRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  @Operation(summary = "플레이리스트 삭제", description = "플레이리스트 소유자만 삭제할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "권한 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<Void> deletePlaylist(
      @Parameter(description = "플레이리스트 UUID") @PathVariable UUID playlistId,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  @Operation(summary = "플레이리스트에 콘텐츠 추가", description = "플레이리스트 소유자만 콘텐츠를 추가할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "권한 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<Void> addContentToPlaylist(
      @Parameter(description = "플레이리스트 UUID") @PathVariable UUID playlistId,
      @Parameter(description = "콘텐츠 UUID") @PathVariable UUID contentId,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  @Operation(summary = "플레이리스트에서 콘텐츠 삭제", description = "플레이리스트 소유자만 콘텐츠를 삭제할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "403", description = "권한 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<Void> removeContentFromPlaylist(
      @Parameter(description = "플레이리스트 UUID") @PathVariable UUID playlistId,
      @Parameter(description = "콘텐츠 UUID") @PathVariable UUID contentId,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  @Operation(summary = "플레이리스트 구독")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<Void> subscribeToPlaylist(
      @Parameter(description = "플레이리스트 UUID") @PathVariable UUID playlistId,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );

  @Operation(summary = "플레이리스트 구독 취소")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "인증 오류", content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "500", description = "서버 오류", content = @Content(schema = @Schema(hidden = true)))
  })
  ResponseEntity<Void> unsubscribeFromPlaylist(
      @Parameter(description = "플레이리스트 UUID") @PathVariable UUID playlistId,
      @Parameter(hidden = true) @AuthenticationPrincipal MoplUserDetails userDetails
  );
}
