package com.mopl.mopl.domain.playlist.repository;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistSubscription;
import com.mopl.mopl.domain.user.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {

  // 사용자와 플레이리스트로 특정 구독 정보를 찾기 위해 사용 (구독 여부 확인, 구독 취소 시)
  Optional<PlaylistSubscription> findByUserAndPlaylist(User user, Playlist playlist);

  boolean existsByUserIdAndPlaylistId(UUID userId, UUID playlistId);

  //플리가 삭제될 때 구독자 정보 모두 삭제
  void deleteAllByPlaylistId(UUID playlistId);

  @Query("SELECT ps FROM PlaylistSubscription ps JOIN FETCH ps.user WHERE ps.playlist.id = :playlistId")
  List<PlaylistSubscription> findAllByPlaylistIdWithUser(@Param("playlistId") UUID playlistId);
}