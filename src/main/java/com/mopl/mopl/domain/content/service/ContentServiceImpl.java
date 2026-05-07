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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ContentServiceImpl implements ContentService
{
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;

    /**
     * 콘텐츠를 생성한다.
     *
     * @param contentCreateRequest  콘텐츠 생성 요청 정보
     * @param thumbnailImage        썸네일 이미지
     * @return 등록된 콘텐츠 정보
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ContentDto create(ContentCreateRequest contentCreateRequest, MultipartFile thumbnailImage) {
        // 나중에 s3에 연결
        String thumbnailKey = (thumbnailImage != null && !thumbnailImage.isEmpty())
                ? thumbnailImage.getOriginalFilename()
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

        return contentMapper.toContentDto(savedContent);
    }

    /**
     * 콘텐츠 ID로 상세 정보를 조회한다.
     *
     * @param contentId 조회할 콘텐츠 ID
     * @return 콘텐츠 정보
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
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
    @Override
    public CursorResponseContentDto getContents(ContentSearchRequest contentSearchRequest) {
        Slice<Content> slice = contentRepository.getContents(contentSearchRequest);
        long totalCount = contentRepository.countContentsWithKeyword(contentSearchRequest.keywordLike());

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
                contentSearchRequest.keywordLike()
        );
    }

    /**
     * 콘텐츠 정보를 수정한다.
     *
     * @param contentId             수정할 콘텐츠 ID
     * @param contentUpdateRequest  콘텐츠 수정 요청 정보
     * @return 수정된 콘텐츠 정보
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public ContentDto update(UUID contentId, ContentUpdateRequest contentUpdateRequest) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        content.update(
                contentUpdateRequest.title(),
                contentUpdateRequest.description(),
                contentUpdateRequest.tags()
        );

        Content savedContent = contentRepository.save(content);

        return contentMapper.toContentDto(savedContent);
    }

    /**
     * 콘텐츠를 삭제한다.
     *
     * @param contentId 삭제할 콘텐츠 ID
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void delete(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        contentRepository.delete(content);
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
