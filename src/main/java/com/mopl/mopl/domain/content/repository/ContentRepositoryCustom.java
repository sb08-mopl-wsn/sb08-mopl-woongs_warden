package com.mopl.mopl.domain.content.repository;

import com.mopl.mopl.domain.content.dto.request.ContentSearchRequest;
import com.mopl.mopl.domain.content.entity.Content;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

public interface ContentRepositoryCustom
{
    Slice<Content> getContents(ContentSearchRequest contentSearchRequest, List<UUID> searchedIds);
    long countContentsWithKeyword(String keywordList);
}
