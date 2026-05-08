package com.mopl.mopl.domain.watchingSession.repository;

import com.mopl.mopl.domain.watchingSession.entity.WatchingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WatchingSessionRepository extends JpaRepository<WatchingSession, UUID> {

    long countByContentId(UUID contentId);

    // JPA에게 조회가 아님을 명시한다. - executeUpdate()
    // DELETE 쿼리는 영속성 컨텍스트를 거치지 않아서, 1차 캐시와 DB가 불일치 할 수 있기 때문에 clearAutomatically = true 사용
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM WatchingSession w WHERE w.content.id = :contentId AND w.user.id = :userId")
    void deleteByContentIdAndUserId(UUID contentId, UUID userId);

    boolean existsByContentIdAndUserId(UUID contentId, UUID userId);
}
