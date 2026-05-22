package com.mopl.mopl.infrastructure.elasticsearch;

import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.infrastructure.elasticsearch.document.ContentDocument;
import com.mopl.mopl.infrastructure.elasticsearch.repository.ContentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContentIndexService
{
    private final ContentSearchRepository contentSearchRepository;

    public boolean index(Content content) {
        try {
            contentSearchRepository.save(ContentDocument.from(content));
            return true;
        } catch (Exception e) {
            log.warn("[ES] 콘텐츠 색인 실패: id={}", content.getId(), e);
            return false;
        }
    }

    public void indexAll(List<Content> contents) {
        try {
            contentSearchRepository.saveAll(
                    contents.stream().map(ContentDocument::from).toList()
            );
        } catch (Exception e) {
            log.warn("[ES] 콘텐츠 일괄 색인 실패: count={}", contents.size(), e);
        }
    }

    public void delete(UUID id) {
        try {
            contentSearchRepository.deleteById(id.toString());
        } catch (Exception e) {
            log.warn("[ES] 콘텐츠 색인 삭제 실패: id={}", id, e);
        }
    }
}
