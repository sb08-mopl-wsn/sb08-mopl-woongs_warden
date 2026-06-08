package com.mopl.mopl.domain.content.service;

import com.mopl.mopl.domain.content.dto.request.ContentSearchRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.mapper.ContentMapper;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.elasticsearch.ContentSearchQueryService;
import com.mopl.mopl.infrastructure.elasticsearch.dto.ContentSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ContentCacheService
{
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final ContentSearchQueryService contentSearchQueryService;

    /**
     * 콘텐츠 ID로 상세 정보를 조회한다.
     *
     * @param contentId 조회할 콘텐츠 ID
     * @return 콘텐츠 정보
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
    @Cacheable(key = "#contentId", value = "content")
    public ContentDto getContent(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        return contentMapper.toContentDto(content);
    }

    /**
     * 콘텐츠 목록을 커서 페이지네이션으로 조회한다.
     *
     * @param contentSearchRequest 검색 정보
     * @return 콘텐츠 목록과 다음 페이지 존재 여부
     */
    @Cacheable(key = "#contentSearchRequest", value = "contents",
            condition = "#contentSearchRequest.cursor() == null && #contentSearchRequest.idAfter() == null")
    public CursorResponseContentDto getContents(ContentSearchRequest contentSearchRequest) {
        List<UUID> searchedIds = null;
        long totalCount = 0;

        if (contentSearchRequest.keywordLike() != null && !contentSearchRequest.keywordLike().isBlank()) {
            ContentSearchResult searchResult = contentSearchQueryService.searchByKeyword(
                    contentSearchRequest.keywordLike(),
                    contentSearchRequest.typeEqual());

            if (searchResult.ids().isEmpty()) {
                return new CursorResponseContentDto(
                        List.of(), null, null, false, 0,
                        contentSearchRequest.sortBy(),
                        contentSearchRequest.sortDirection());
            }

            searchedIds = searchResult.ids();
            totalCount = searchResult.totalHits();
        } else {
            totalCount = contentRepository.countContentsWithKeyword(null);
        }

        Slice<Content> slice = contentRepository.getContents(contentSearchRequest, searchedIds);

        List<ContentDto> contents = contentMapper.toContentDtos(slice.getContent());

        String nextCursor = null;
        UUID nextIdAfter = null;

        if (slice.hasNext()) {
            Content last = slice.getContent().getLast();
            nextCursor = extractCursor(last, contentSearchRequest.sortBy());
            nextIdAfter = last.getId();
        }

        return new CursorResponseContentDto(
                contents,
                nextCursor,
                nextIdAfter,
                slice.hasNext(),
                totalCount,
                contentSearchRequest.sortBy(),
                contentSearchRequest.sortDirection());
    }

    private String extractCursor(Content content, String sortBy) {
        return switch (sortBy == null ? "watcherCount" : sortBy) {
            case "avgRating" -> content.getAvgRating().toString();
            case "reviewCount" -> String.valueOf(content.getReviewCount());
            case "createdAt" -> content.getCreatedAt().toString();
            default -> String.valueOf(content.getWatcherCount());
        };
    }
}
