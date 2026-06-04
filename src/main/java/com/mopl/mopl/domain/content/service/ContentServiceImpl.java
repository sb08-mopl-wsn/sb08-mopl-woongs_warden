package com.mopl.mopl.domain.content.service;

import com.mopl.mopl.domain.content.dto.request.ContentCreateRequest;
import com.mopl.mopl.domain.content.dto.request.ContentSearchRequest;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.dto.response.CursorResponseContentDto;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.mapper.ContentMapper;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import com.mopl.mopl.infrastructure.ai.service.ContentEmbeddingService;
import com.mopl.mopl.infrastructure.elasticsearch.ContentSearchQueryService;
import com.mopl.mopl.infrastructure.elasticsearch.dto.ContentSearchResult;
import com.mopl.mopl.infrastructure.kafka.event.ContentDeleteEvent;
import com.mopl.mopl.infrastructure.kafka.event.ContentIndexEvent;
import com.mopl.mopl.infrastructure.s3.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ContentServiceImpl implements ContentService
{
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final S3ImageStorage s3ImageStorage;
    private final ContentSearchQueryService contentSearchQueryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ContentEmbeddingService contentEmbeddingService;

    /**
     * 콘텐츠를 생성한다.
     *
     * @param contentCreateRequest  콘텐츠 생성 요청 정보
     * @param thumbnailImage        썸네일 이미지
     * @return 등록된 콘텐츠 정보
     */
    @CacheEvict(value = "contents", allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ContentDto create(ContentCreateRequest contentCreateRequest, MultipartFile thumbnailImage) {
        // DB 롤백에 맞게 S3를 맞춰야 하지만, 업로드는 고아 파일이 남아도 치명적이지 않음.
        String thumbnailKey = (thumbnailImage != null && !thumbnailImage.isEmpty())
                ? s3ImageStorage.upload(thumbnailImage, "thumbnail")
                : null;

        Content content = Content.builder()
                .title(contentCreateRequest.title())
                .description(contentCreateRequest.description())
                .contentType(ContentType.valueOf(contentCreateRequest.type()))
                .releaseDate(null)
                .thumbnailKey(thumbnailKey)
                .tags(contentCreateRequest.tags())
                .build();

        Content savedContent = contentRepository.save(content);

        applicationEventPublisher.publishEvent(new ContentIndexEvent(savedContent));

        try {
            contentEmbeddingService.generateAndSave(savedContent, "api");
        } catch (Exception e) {
            log.warn("콘텐츠 임베딩 생성 실패: contentId={}", savedContent.getId(), e);
        }

        return contentMapper.toContentDto(savedContent);
    }

    /**
     * 콘텐츠 ID로 상세 정보를 조회한다.
     *
     * @param contentId 조회할 콘텐츠 ID
     * @return 콘텐츠 정보
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
    @Cacheable(key = "#contentId", value = "content")
    @Override
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
    @Override
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

    /**
     * 콘텐츠 정보를 수정한다.
     *
     * @param contentId             수정할 콘텐츠 ID
     * @param contentUpdateRequest  콘텐츠 수정 요청 정보
     * @param thumbnailImage        썸네일 이미지
     * @return 수정된 콘텐츠 정보
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
    @CacheEvict(value = {"content", "contents"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ContentDto update(UUID contentId, ContentUpdateRequest contentUpdateRequest, MultipartFile thumbnailImage) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        String thumbnailKey = (thumbnailImage != null && !thumbnailImage.isEmpty())
                ? s3ImageStorage.upload(thumbnailImage, "thumbnail")
                : null;

        content.update(
                thumbnailKey,
                contentUpdateRequest.title(),
                contentUpdateRequest.description(),
                contentUpdateRequest.tags()
        );

        Content savedContent = contentRepository.save(content);

        applicationEventPublisher.publishEvent(new ContentIndexEvent(savedContent));

        return contentMapper.toContentDto(savedContent);
    }

    /**
     * 콘텐츠를 삭제한다.
     *
     * @param contentId 삭제할 콘텐츠 ID
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
    @CacheEvict(value = {"content", "contents"}, allEntries = true)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void delete(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        contentRepository.delete(content);

        applicationEventPublisher.publishEvent(new ContentDeleteEvent(contentId, content.getThumbnailKey()));
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
