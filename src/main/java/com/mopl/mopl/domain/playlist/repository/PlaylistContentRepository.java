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

  // 단건 조회용: 특정 플레이리스트 1개에 담긴 콘텐츠 목록을 가져옴
  @EntityGraph(attributePaths = {"content"})
  List<PlaylistContent> findAllByPlaylistId(UUID playlistId);

  // 다건 조회용: 여러 플레이리스트의 ID를 IN 쿼리로 한 번에 던져서 모든 콘텐츠를 가져옴
  @EntityGraph(attributePaths = {"content"})
  List<PlaylistContent> findAllByPlaylistIdIn(List<UUID> playlistIds);

  //플리가 삭제될 때 담겨있던 콘텐츠 연결 정보 모두 삭제
  void deleteAllByPlaylistId(UUID playlistId);
}