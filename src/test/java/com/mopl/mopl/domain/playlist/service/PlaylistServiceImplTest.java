package com.mopl.mopl.domain.playlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.mopl.domain.content.dto.response.ContentSummary;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
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
import com.mopl.mopl.domain.playlist.service.impl.PlaylistServiceImpl;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.domain.user.exception.UserNotFoundException;
import com.mopl.mopl.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaylistService Unit Test")
class PlaylistServiceImplTest {

  @InjectMocks
  private PlaylistServiceImpl playlistService;

  @Mock private PlaylistRepository playlistRepository;
  @Mock private UserRepository userRepository;
  @Mock private PlaylistMapper playlistMapper;
  @Mock private PlaylistSubscriptionRepository playlistSubscriptionRepository;
  @Mock private PlaylistContentRepository playlistContentRepository;
  @Mock private ContentMapper contentMapper;

  private User user;
  private Playlist playlist;
  private UUID userId;
  private UUID playlistId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    playlistId = UUID.randomUUID();

    user = User.builder().name("test user").email("test@test.com").build();
    ReflectionTestUtils.setField(user, "id", userId);

    playlist = Playlist.builder().user(user).title("test playlist").description("desc").build();
    ReflectionTestUtils.setField(playlist, "id", playlistId);
    ReflectionTestUtils.setField(playlist, "subscriberCount", 5L);
    ReflectionTestUtils.setField(playlist, "updatedAt", Instant.parse("2026-05-19T12:00:00Z"));
  }

  @Nested
  @DisplayName("플레이리스트 생성")
  class Create {

    @Test
    @DisplayName("정상적으로 플레이리스트를 생성한다.")
    void givenValidRequest_whenCreate_thenSuccess() {
      // given
      PlaylistCreateRequest request = new PlaylistCreateRequest("test playlist", "desc");
      PlaylistDto expectedDto = new PlaylistDto(playlistId, null, "test playlist", "desc", null, 0L, false, Collections.emptyList());

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(playlistRepository.save(any(Playlist.class))).willReturn(playlist);
      given(playlistMapper.toDto(playlist)).willReturn(expectedDto);

      // when
      PlaylistDto result = playlistService.createPlaylist(request, userId);

      // then
      assertThat(result.title()).isEqualTo("test playlist");
      assertThat(result.contents()).isEmpty();
      then(playlistRepository).should().save(any(Playlist.class));
      then(playlistMapper).should().toDto(playlist);
    }

    @Test
    @DisplayName("유저가 존재하지 않으면 예외가 발생한다.")
    void givenNonExistingUser_whenCreate_thenThrowsException() {
      // given
      PlaylistCreateRequest request = new PlaylistCreateRequest("test playlist", "desc");
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> playlistService.createPlaylist(request, userId))
          .isInstanceOf(UserNotFoundException.class);
    }
  }
  @Nested
  @DisplayName("플레이리스트 단건 조회")
  class FindPlaylist {
    @Test
    @DisplayName("로그인한 유저가 존재하는 플레이리스트를 상세 조회하면 구독 여부와 콘텐츠를 포함해 반환한다.")
    void givenExistingIdAndUser_whenFindById_thenSuccess() {
      // given
      Content content = Content.builder().title("영화").contentType(ContentType.movie).build();
      PlaylistContent pc = new PlaylistContent(playlist, content);
      ContentSummary summary = new ContentSummary(UUID.randomUUID(), ContentType.movie, "영화", "설명", "thumb", List.of("tag"), BigDecimal.ZERO, 0);
      PlaylistDto expectedDto = new PlaylistDto(playlistId, null, "test playlist", "desc", null, 5L, true, List.of(summary));

      given(playlistRepository.findByIdWithUser(playlistId)).willReturn(Optional.of(playlist));
      given(playlistSubscriptionRepository.existsByUserIdAndPlaylistId(userId, playlistId)).willReturn(true);
      given(playlistContentRepository.findAllByPlaylistId(playlistId)).willReturn(List.of(pc));
      given(contentMapper.toContentSummary(content)).willReturn(summary);
      given(playlistMapper.toDto(eq(playlist), eq(true), anyList())).willReturn(expectedDto);

      // when
      PlaylistDto result = playlistService.findPlaylistById(playlistId, userId);

      // then
      assertThat(result.subscribedByMe()).isTrue();
      assertThat(result.contents()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트를 조회하면 예외가 발생한다.")
    void givenNonExistingId_whenFindById_thenThrowsException() {
      // given
      given(playlistRepository.findByIdWithUser(playlistId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> playlistService.findPlaylistById(playlistId, userId))
          .isInstanceOf(PlaylistNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("플레이리스트 다건 조회 (목록 검색)")
  class FindPlaylists {
    @Test
    @DisplayName("정상적으로 검색 쿼리를 던지면 인덱스 기반 커서 정보를 묶어 반환한다.")
    void givenSearchRequest_whenFindPlaylists_thenReturnsCursorDto() {
      // given
      PlaylistSearchRequest request = new PlaylistSearchRequest(null, null, null, 10, "subscriberCount", "DESCENDING", null, null);
      Slice<Playlist> slice = new SliceImpl<>(List.of(playlist), Pageable.unpaged(), true);
      Content content = Content.builder().title("영화").contentType(ContentType.movie).build();
      PlaylistContent pc = new PlaylistContent(playlist, content);
      ContentSummary summary = new ContentSummary(UUID.randomUUID(), ContentType.movie, "영화", "설명", "thumb", List.of("tag"), BigDecimal.ZERO, 0);
      PlaylistDto playlistDto = new PlaylistDto(playlistId, null, "test playlist", "desc", null, 5L, false, List.of(summary));

      ReflectionTestUtils.setField(pc, "playlist", playlist);
      ReflectionTestUtils.setField(pc, "content", content);

      given(playlistRepository.findPlaylists(request)).willReturn(slice);
      given(playlistContentRepository.findAllByPlaylistIdIn(anyList())).willReturn(List.of(pc));
      given(contentMapper.toContentSummary(content)).willReturn(summary);
      given(playlistMapper.toDto(eq(playlist), eq(false), anyList())).willReturn(playlistDto);
      given(playlistRepository.countPlaylists(request)).willReturn(100L);

      // when
      CursorResponsePlaylistDto result = playlistService.findPlaylists(request);

      // then
      assertThat(result.data()).hasSize(1);
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo("5");
      assertThat(result.nextIdAfter()).isEqualTo(playlistId);
      assertThat(result.totalCount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("정렬 기준이 없으면 updatedAt 기반 커서 정보를 반환한다.")
    void givenNoSortBy_whenFindPlaylists_thenReturnsUpdatedAtCursor() {
      // given
      PlaylistSearchRequest request = new PlaylistSearchRequest(null, null, null, 10, null, "DESCENDING", null, null);
      Slice<Playlist> slice = new SliceImpl<>(List.of(playlist), Pageable.unpaged(), true);
      Content content = Content.builder().title("영화").contentType(ContentType.movie).build();
      PlaylistContent pc = new PlaylistContent(playlist, content);
      ContentSummary summary = new ContentSummary(UUID.randomUUID(), ContentType.movie, "영화", "설명", "thumb", List.of("tag"), BigDecimal.ZERO, 0);
      PlaylistDto playlistDto = new PlaylistDto(playlistId, null, "test playlist", "desc", null, 5L, false, List.of(summary));

      ReflectionTestUtils.setField(pc, "playlist", playlist);
      ReflectionTestUtils.setField(pc, "content", content);

      given(playlistRepository.findPlaylists(request)).willReturn(slice);
      given(playlistContentRepository.findAllByPlaylistIdIn(anyList())).willReturn(List.of(pc));
      given(contentMapper.toContentSummary(content)).willReturn(summary);
      given(playlistMapper.toDto(eq(playlist), eq(false), anyList())).willReturn(playlistDto);
      given(playlistRepository.countPlaylists(request)).willReturn(100L);

      // when
      CursorResponsePlaylistDto result = playlistService.findPlaylists(request);

      // then
      assertThat(result.hasNext()).isTrue();

      assertThat(result.nextCursor()).isEqualTo(playlist.getUpdatedAt().toString());
    }
  }

  @Nested
  @DisplayName("플레이리스트 수정")
  class UpdatePlaylist {
    @Test
    @DisplayName("소유자가 수정을 요청하면 정상적으로 반영된다.")
    void givenOwnerRequest_whenUpdate_thenSuccess() {
      // given
      PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정 제목", "수정 설명");
      PlaylistDto expectedDto = new PlaylistDto(playlistId, null, "수정 제목", "수정 설명", null, 5L, false, Collections.emptyList());

      given(playlistRepository.findByIdWithUser(playlistId)).willReturn(Optional.of(playlist));
      given(playlistMapper.toDto(playlist)).willReturn(expectedDto);

      // when
      PlaylistDto result = playlistService.updatePlaylist(playlistId, request, userId);

      // then
      assertThat(result.title()).isEqualTo("수정 제목");
    }

    @Test
    @DisplayName("소유자가 아닌 유저가 수정을 요청하면 예외가 발생한다.")
    void givenNotOwner_whenUpdate_thenThrowsException() {
      // given
      PlaylistUpdateRequest request = new PlaylistUpdateRequest("수정 제목", "수정 설명");
      UUID otherUserId = UUID.randomUUID();
      given(playlistRepository.findByIdWithUser(playlistId)).willReturn(Optional.of(playlist));

      // when & then
      assertThatThrownBy(() -> playlistService.updatePlaylist(playlistId, request, otherUserId))
          .isInstanceOf(PlaylistForbiddenException.class);
    }
  }

  @Nested
  @DisplayName("플레이리스트 삭제")
  class DeletePlaylist {
    @Test
    @DisplayName("소유자가 삭제를 요청하면 연관 매핑 및 구독 정보를 먼저 날리고 플레이리스트를 제거한다.")
    void givenOwnerRequest_whenDelete_thenSuccess() {
      // given
      given(playlistRepository.findByIdWithUser(playlistId)).willReturn(Optional.of(playlist));

      // when
      playlistService.deletePlaylist(playlistId, userId);

      // then
      then(playlistContentRepository).should().deleteAllByPlaylistId(playlistId);
      then(playlistSubscriptionRepository).should().deleteAllByPlaylistId(playlistId);
      then(playlistRepository).should().delete(playlist);
    }
  }
}