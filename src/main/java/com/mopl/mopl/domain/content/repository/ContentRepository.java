package com.mopl.mopl.domain.content.repository;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom
{
    boolean existsByExternalIdAndContentType(String externalId, ContentType contentType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Content c WHERE c.id = :contentId")
    Optional<Content> findByIdForUpdate(@Param("contentId") UUID contentId);
}
