package com.mopl.mopl.domain.content.service;

import com.mopl.mopl.domain.content.dto.request.ContentCreateRequest;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.entity.Content;
import com.mopl.mopl.domain.content.entity.ContentType;
import com.mopl.mopl.domain.content.exception.ContentNotFoundException;
import com.mopl.mopl.domain.content.mapper.ContentMapper;
import com.mopl.mopl.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
     * 콘텐츠 정보를 수정한다.
     *
     * @param contentId             수정할 콘텐츠 ID
     * @param contentUpdateRequest  콘텐츠 수정 요청 정보
     * @return 수정된 콘텐츠 정보
     * @throws ContentNotFoundException 콘텐츠가 존재하지 않을 때
     */
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
    @Transactional
    @Override
    public void delete(UUID contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ContentNotFoundException(contentId));

        contentRepository.delete(content);
    }
}
