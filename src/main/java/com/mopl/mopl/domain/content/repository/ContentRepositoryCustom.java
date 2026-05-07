package com.mopl.mopl.domain.content.repository;

import com.mopl.mopl.domain.content.dto.request.ContentSearchRequest;
import com.mopl.mopl.domain.content.entity.Content;
import org.springframework.data.domain.Slice;

public interface ContentRepositoryCustom
{
    Slice<Content> getContents(ContentSearchRequest contentSearchRequest);
    long countContentsWithKeyword(String keywordList);
}
