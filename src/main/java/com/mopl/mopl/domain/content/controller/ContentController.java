package com.mopl.mopl.domain.content.controller;

import com.mopl.mopl.domain.content.dto.request.ContentCreateRequest;
import com.mopl.mopl.domain.content.dto.request.ContentUpdateRequest;
import com.mopl.mopl.domain.content.dto.response.ContentDto;
import com.mopl.mopl.domain.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/contents")
public class ContentController
{
    private final ContentService contentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContentDto> createContent(@Valid @RequestPart("request") ContentCreateRequest contentCreateRequest,
                                                    @RequestPart(value = "thumbnail", required = true) MultipartFile thumbnailImage)
    {
        ContentDto contentDto = contentService.create(contentCreateRequest, thumbnailImage);

        return ResponseEntity.status(HttpStatus.CREATED).body(contentDto);
    }

    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDto> getContent(@PathVariable UUID contentId)
    {
        ContentDto contentDto = contentService.getContent(contentId);

        return ResponseEntity.status(HttpStatus.OK).body(contentDto);
    }

    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentDto> updateContent(@PathVariable UUID contentId,
                                                    @Valid @RequestPart("request") ContentUpdateRequest contentUpdateRequest,
                                                    @RequestPart(value = "thumbnail", required = true) MultipartFile thumbnailImage)
    {
        ContentDto contentDto = contentService.update(contentId, contentUpdateRequest, thumbnailImage);

        return ResponseEntity.status(HttpStatus.OK).body(contentDto);
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(@PathVariable UUID contentId)
    {
        contentService.delete(contentId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
