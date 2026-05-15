package com.mopl.mopl.domain.playlist.repository;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistContent;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlaylistContentRepository extends JpaRepository<PlaylistContent, UUID> {

  // 플레이리스트와 콘텐츠 ID로 특정 항목을 찾기 위해 사용 (콘텐츠 삭제 시)
  Optional<PlaylistContent> findByPlaylistAndContentId(Playlist playlist, UUID contentId);

  @EntityGraph(attributePaths = {"content"})
  List<PlaylistContent> findAllByPlaylistId(UUID playlistId);

  //플리가 삭제될 때 담겨있던 콘텐츠 연결 정보 모두 삭제
  void deleteAllByPlaylistId(UUID playlistId);
}