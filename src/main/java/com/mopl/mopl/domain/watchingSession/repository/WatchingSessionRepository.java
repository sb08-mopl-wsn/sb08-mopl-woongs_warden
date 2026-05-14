package com.mopl.mopl.domain.watchingSession.repository;

import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID>, WatchingSessionRepositoryCustom {

    // 시청자 수를 카운트
    long countByContentId(UUID contentId);

    // 이미 시청 중인지를 확인
    boolean existsByContentIdAndUserId(UUID contentId, UUID userId);

    // 특정 세션 조회
    Optional<WatchingSession> findByContentIdAndUserId(UUID contentId, UUID userId);

    // 하나의 사용자가 다른 콘텐츠를 시청은 가능하므로 최신 기준으로 하나 가져옵니다.
    @Query("""
            SELECT ws FROM WatchingSession ws
            JOIN FETCH ws.user
            JOIN FETCH ws.content
            WHERE ws.user.id = :userId
            ORDER BY ws.createdAt DESC
            LIMIT 1
    """)
    Optional<WatchingSession> findByUserId(@Param("userId") UUID userId);
}
