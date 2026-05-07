package com.mopl.mopl.domain.content.service;

import com.mopl.mopl.domain.content.dto.request.ContentCreateRequest;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ContentService
{
    ContentDto create(ContentCreateRequest contentCreateRequest, MultipartFile thumbnailImage);
    ContentDto getContent(UUID contentId);
    ContentDto update(UUID contentId, ContentUpdateRequest contentUpdateRequest, MultipartFile thumbnailImage);
    void delete(UUID contentId);
}
