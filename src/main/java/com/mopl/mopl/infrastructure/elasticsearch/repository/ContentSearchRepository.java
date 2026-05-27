package com.mopl.mopl.infrastructure.elasticsearch.repository;

import com.mopl.mopl.infrastructure.elasticsearch.document.ContentDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentSearchRepository extends ElasticsearchRepository<ContentDocument, String>
{
}
