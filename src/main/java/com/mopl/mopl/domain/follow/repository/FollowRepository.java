package com.mopl.mopl.domain.follow.repository;

import com.mopl.mopl.domain.follow.entity.Follow;
import com.mopl.mopl.domain.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

  Optional<Follow> findByFollowerAndFollowee(User follower, User followee);

  // 다른 사람 팔로우 내역 삭제 시도 방지용
  Optional<Follow> findByIdAndFollowerId(UUID id, UUID followerId);

  // User 객체 대신 ID로 조회용
  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
  long countByFolloweeId(UUID followeeId);

  // 특정 유저를 팔로우하는 모든 팔로우 관계 찾기
  List<Follow> findAllByFolloweeId(UUID followeeId);
}
