package com.mopl.mopl.domain.watchingSession.repository;

import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID> {

    // 시청자 수를 카운트
    long countByContentId(UUID contentId);

    // 이미 시청 중인지를 확인
    boolean existsByContentIdAndUserId(UUID contentId, UUID userId);

    // 특정 세션 조회
    Optional<WatchingSession> findByContentIdAndUserId(UUID contentId, UUID userId);
}
