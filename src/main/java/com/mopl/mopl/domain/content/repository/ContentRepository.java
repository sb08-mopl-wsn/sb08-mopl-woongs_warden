package com.mopl.mopl.domain.content.repository;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID>, ContentRepositoryCustom
{
    boolean existsByExternalIdAndContentType(String externalId, ContentType contentType);
}
