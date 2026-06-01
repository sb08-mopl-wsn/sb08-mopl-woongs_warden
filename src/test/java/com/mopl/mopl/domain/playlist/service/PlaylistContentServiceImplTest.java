package com.mopl.mopl.domain.playlist.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistContent;
import com.mopl.mopl.domain.playlist.exception.ContentAlreadyInPlaylistException;
import com.mopl.mopl.domain.playlist.exception.ContentNotFoundInPlaylistException;
import com.mopl.mopl.domain.playlist.exception.PlaylistForbiddenException;
import com.mopl.mopl.domain.playlist.exception.PlaylistNotFoundException;
import com.mopl.mopl.domain.playlist.exception.PlaylistUpdateFailedException;
import com.mopl.mopl.domain.playlist.repository.PlaylistContentRepository;
import com.mopl.mopl.domain.playlist.repository.PlaylistRepository;
import com.mopl.mopl.domain.playlist.service.impl.PlaylistContentServiceImpl;
import com.mopl.mopl.domain.user.entity.User;
import com.mopl.mopl.global.event.PlaylistContentAddedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaylistContentService Unit Test")
class PlaylistContentServiceImplTest {

  @InjectMocks
  private PlaylistContentServiceImpl playlistContentService;

  @Mock
  private PlaylistRepository playlistRepository;
  @Mock
  private ContentRepository contentRepository;
  @Mock
  private PlaylistContentRepository playlistContentRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  private User owner;
  private User otherUser;
  private Playlist playlist;
  private Content content;
  private UUID playlistId;
  private UUID contentId;
  private UUID ownerId;
  private UUID otherUserId;

  @BeforeEach
  void setUp() {
    ownerId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();
    playlistId = UUID.randomUUID();
    contentId = UUID.randomUUID();

    owner = User.builder().name("owner").email("owner@test.com").build();
    ReflectionTestUtils.setField(owner, "id", ownerId);

    otherUser = User.builder().name("other").email("other@test.com").build();
    ReflectionTestUtils.setField(otherUser, "id", otherUserId);

    playlist = Playlist.builder().user(owner).title("test playlist").description("desc").build();
    ReflectionTestUtils.setField(playlist, "id", playlistId);
    ReflectionTestUtils.setField(playlist, "contentCount", 1L);

    content = Content.builder().title("test content").contentType(ContentType.movie).build();
    ReflectionTestUtils.setField(content, "id", contentId);
  }

  @Nested
  @DisplayName("플레이리스트 콘텐츠 추가")
  class AddContent {

    @Test
    @DisplayName("정상적으로 콘텐츠를 추가하고 카운트를 증가시킨다.")
    void givenValidRequest_whenAddContent_thenSuccess() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
      given(playlistRepository.increaseContentCount(playlistId)).willReturn(1);

      // when
      playlistContentService.addContentToPlaylist(playlistId, contentId, ownerId);

      // then
      then(playlistContentRepository).should().saveAndFlush(any(PlaylistContent.class));
      then(playlistRepository).should().increaseContentCount(playlistId);
      then(eventPublisher).should().publishEvent(any(PlaylistContentAddedEvent.class));
    }

    @Test
    @DisplayName("플레이리스트 주인이 아니면 예외가 발생한다.")
    void givenNotOwner_whenAddContent_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));

      // when & then
      assertThatThrownBy(() -> playlistContentService.addContentToPlaylist(playlistId, contentId, otherUserId))
          .isInstanceOf(PlaylistForbiddenException.class);
    }

    @Test
    @DisplayName("콘텐츠가 존재하지 않으면 예외가 발생한다.")
    void givenNonExistingContent_whenAddContent_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(contentRepository.findById(contentId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> playlistContentService.addContentToPlaylist(playlistId, contentId, ownerId))
          .isInstanceOf(ContentNotFoundException.class);
    }

    @Test
    @DisplayName("이미 플레이리스트에 있는 콘텐츠를 추가하면 예외가 발생한다.")
    void givenAlreadyAddedContent_whenAddContent_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
      given(playlistContentRepository.saveAndFlush(any(PlaylistContent.class)))
          .willThrow(new DataIntegrityViolationException("duplicate"));

      // when & then
      assertThatThrownBy(() -> playlistContentService.addContentToPlaylist(playlistId, contentId, ownerId))
          .isInstanceOf(ContentAlreadyInPlaylistException.class);
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트에 콘텐츠를 추가하려 하면 예외가 발생한다.")
    void givenNonExistingPlaylist_whenAddContent_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> playlistContentService.addContentToPlaylist(playlistId, contentId, ownerId))
          .isInstanceOf(PlaylistNotFoundException.class);
    }

    @Test
    @DisplayName("카운트 증가 쿼리가 실패하면 예외가 발생한다.")
    void givenIncreaseCountFails_whenAddContent_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(contentRepository.findById(contentId)).willReturn(Optional.of(content));

      given(playlistRepository.increaseContentCount(playlistId)).willReturn(0);

      // when & then
      assertThatThrownBy(() -> playlistContentService.addContentToPlaylist(playlistId, contentId, ownerId))
          .isInstanceOf(PlaylistUpdateFailedException.class);
    }
  }

  @Nested
  @DisplayName("플레이리스트 콘텐츠 삭제")
  class RemoveContent {

    @Test
    @DisplayName("정상적으로 콘텐츠를 삭제하고 카운트를 감소시킨다.")
    void givenValidRequest_whenRemoveContent_thenSuccess() {
      // given
      PlaylistContent playlistContent = new PlaylistContent(playlist, content);
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(playlistContentRepository.findByPlaylistAndContentId(playlist, contentId))
          .willReturn(Optional.of(playlistContent));

      // when
      playlistContentService.removeContentFromPlaylist(playlistId, contentId, ownerId);

      // then
      then(playlistContentRepository).should().delete(playlistContent);
      then(playlistContentRepository).should().flush();
      then(playlistRepository).should().decreaseContentCount(playlistId);
    }

    @Test
    @DisplayName("플레이리스트에 없는 콘텐츠를 삭제하려 하면 예외가 발생한다.")
    void givenContentNotInPlaylist_whenRemoveContent_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.of(playlist));
      given(playlistContentRepository.findByPlaylistAndContentId(playlist, contentId))
          .willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> playlistContentService.removeContentFromPlaylist(playlistId, contentId, ownerId))
          .isInstanceOf(ContentNotFoundInPlaylistException.class);
    }

    @Test
    @DisplayName("존재하지 않는 플레이리스트에서 콘텐츠를 삭제하려 하면 예외가 발생한다.")
    void givenNonExistingPlaylist_whenRemoveContent_thenThrowsException() {
      // given
      given(playlistRepository.findByIdForUpdate(playlistId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> playlistContentService.removeContentFromPlaylist(playlistId, contentId, ownerId))
          .isInstanceOf(PlaylistNotFoundException.class);
    }
  }
}