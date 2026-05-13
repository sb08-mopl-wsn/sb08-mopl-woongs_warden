package com.mopl.mopl.domain.playlist.repository;

import com.mopl.mopl.domain.playlist.entity.Playlist;
import com.mopl.mopl.domain.playlist.entity.PlaylistSubscription;
import com.mopl.mopl.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {

  // 사용자와 플레이리스트로 특정 구독 정보를 찾기 위해 사용 (구독 여부 확인, 구독 취소 시)
  Optional<PlaylistSubscription> findByUserAndPlaylist(User user, Playlist playlist);
}