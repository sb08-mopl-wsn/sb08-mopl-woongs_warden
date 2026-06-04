package com.mopl.mopl.domain.content.repository;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom
{
    @Query(value = """
    SELECT * FROM (
        SELECT *, embedding <=> CAST(:embedding AS vector) AS distance
        FROM contents
        WHERE embedding IS NOT NULL
        AND id NOT IN (SELECT content_id FROM reviews WHERE user_id = CAST(:userId AS uuid))
        AND embedding <=> CAST(:embedding AS vector) < 0.3
        ORDER BY distance ASC
        LIMIT :pool
    ) sub
    ORDER BY RANDOM()
    LIMIT :limit
    """, nativeQuery = true)
    List<Content> findSimilarContents(
            @Param("embedding") String embedding,
            @Param("pool") int pool,
            @Param("limit") int limit,
            @Param("userId") String userId
    );

    @Query(value = """
    SELECT AVG(embedding)::text
    FROM contents
    WHERE id IN (
        SELECT content_id FROM reviews
        WHERE user_id = CAST(:userId AS uuid)
        AND rating >= :minRating
    )
    """, nativeQuery = true)
    String findAvgEmbeddingByUserId(
            @Param("userId") String userId,
            @Param("minRating") double minRating
    );

    boolean existsByExternalIdAndContentType(String externalId, ContentType contentType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT c FROM Content c WHERE c.id = :contentId")
    Optional<Content> findByIdForUpdate(@Param("contentId") UUID contentId);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE contents " +
                   "SET watcher_count = 0 " +
                   "WHERE id IN (" +
                   "    SELECT id FROM contents " +
                   "    WHERE watcher_count > 0 " +
                   "    LIMIT :limit" +
                   ")",  nativeQuery = true)
    int resetWatcherCountsInBatches(@Param("limit") int limit);
}
