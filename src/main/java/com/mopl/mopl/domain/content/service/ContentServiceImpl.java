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
import com.mopl.mopl.infrastructure.kafka.event.ContentDeleteEvent;
import com.mopl.mopl.infrastructure.kafka.event.ContentIndexEvent;
import com.mopl.mopl.infrastructure.s3.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ContentServiceImpl implements ContentService
{
    private final ContentRepository contentRepository;
    private final ContentMapper contentMapper;
    private final S3ImageStorage s3ImageStorage;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ContentEmbeddingService contentEmbeddingService;
    private final ContentCacheService contentCacheService;
    private final StringRedisTemplate redisTemplate;

    public static final String REDIS_KEY_PREFIX = "content:watcher:count:";

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

    public ContentDto getContentWithLiveCount(UUID contentId) {
        ContentDto dto = contentCacheService.getContent(contentId);
        String raw = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + contentId);
        return raw != null ? dto.withWatcherCount(Integer.parseInt(raw)) : dto;
    }

    @Override
    public CursorResponseContentDto getContentsWithLiveCount(ContentSearchRequest contentSearchRequest) {
        CursorResponseContentDto response = contentCacheService.getContents(contentSearchRequest);

        List<String> keys = response.data().stream()
                .map(c -> REDIS_KEY_PREFIX + c.id())
                .toList();

        List<String> counts = redisTemplate.opsForValue().multiGet(keys);
        if (counts == null) return response;

        List<ContentDto> enriched = IntStream.range(0, response.data().size())
                .mapToObj(i -> {
                    String raw = counts.get(i);
                    return raw != null
                            ? response.data().get(i).withWatcherCount(Integer.parseInt(raw))
                            : response.data().get(i);
                })
                .toList();

        return response.withContents(enriched);
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
}
