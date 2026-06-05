package com.mopl.mopl.domain.watchingSession.repository;

import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID>, WatchingSessionRepositoryCustom {

    // 시청자 수를 카운트
    long countByContentId(UUID contentId);

    // 특정 세션 조회
    Optional<WatchingSession> findByContentIdAndUserId(UUID contentId, UUID userId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM watching_sessions " +
                   "WHERE id IN (" +
                   "    SELECT id FROM watching_sessions " +
                   "    LIMIT  :limit" +
                   ")", nativeQuery = true)
    int deleteSessionsInBatches(@Param("limit") int limit);
}
