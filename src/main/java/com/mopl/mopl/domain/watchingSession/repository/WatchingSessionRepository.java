package com.mopl.mopl.domain.watchingSession.repository;

import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID> {

    long countByContentId(UUID contentId);

    void deleteByContentIdAndUserId(UUID contentId, UUID userId);

    boolean existsByContentIdAndUserId(UUID contentId, UUID userId);
}
